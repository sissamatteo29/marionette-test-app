package com.outfit.processor.controllers;

import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/process")
public class ImageProcessingController {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String imageStoreBaseUrl = "http://localhost:8080"; // adjust port

    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    @GetMapping("/one")
    public ResponseEntity<byte[]> processSingleImage(@RequestParam("filename") String fileName) {
        try {
            ProcessedImageResponse processed = fetchAndProcess(fileName);

            byte[] imageBytes = Base64.getDecoder().decode(processed.getBase64Content());

            return ResponseEntity.ok()
                    .header("Content-Disposition", "inline; filename=\"" + fileName + "\"")
                    .header("Content-Type", "image/jpeg")
                    .body(imageBytes);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<ProcessedImageResponse>> processImages(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "4") int size) {

        // 1. Fetch image metadata (list of filenames)
        String metadataUrl = imageStoreBaseUrl + "/images?page=" + page + "&size=" + size;
        List<Map<String, String>> metadata
                = restTemplate.getForObject(metadataUrl, List.class);

        if (metadata == null || metadata.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        // 2. Process images in parallel
        List<CompletableFuture<ProcessedImageResponse>> futures = metadata.stream()
                .map(img -> CompletableFuture.supplyAsync(() -> fetchAndProcess(img.get("fileName")), executor))
                .collect(Collectors.toList());

        // 3. Collect results
        List<ProcessedImageResponse> results = futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());

        return ResponseEntity.ok(results);
    }

    private ProcessedImageResponse fetchAndProcess(String fileName) {
        try {
            String imageUrl = imageStoreBaseUrl + "/image/" + fileName;
            Resource resource = restTemplate.getForObject(imageUrl, Resource.class);

            if (resource == null) {
                throw new RuntimeException("Could not fetch image: " + fileName);
            }

            try (InputStream in = resource.getInputStream()) {
                BufferedImage image = ImageIO.read(in);

                // Step 1: Brightness and contrast adjustment
                BufferedImage adjusted = adjustBrightnessContrast(image, 1.2, 1.1);

                // Step 2: Saturation boost
                BufferedImage saturated = adjustSaturation(adjusted, 1.3);

                // Step 3: Sharpen filter
                BufferedImage sharpened = applySharpen(saturated);

                // Encode processed image to Base64
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(sharpened, "jpeg", baos);
                String base64 = Base64.getEncoder().encodeToString(baos.toByteArray());

                return new ProcessedImageResponse(fileName, base64);
            }

        } catch (Exception e) {
            throw new RuntimeException("Error processing image " + fileName, e);
        }
    }

    private BufferedImage adjustBrightnessContrast(BufferedImage img, double brightness, double contrast) {
        BufferedImage result = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);

        for (int x = 0; x < img.getWidth(); x++) {
            for (int y = 0; y < img.getHeight(); y++) {
                int rgb = img.getRGB(x, y);

                int r = (int) (((rgb >> 16) & 0xFF) * brightness);
                int g = (int) (((rgb >> 8) & 0xFF) * brightness);
                int b = (int) ((rgb & 0xFF) * brightness);

                r = (int) ((r - 128) * contrast + 128);
                g = (int) ((g - 128) * contrast + 128);
                b = (int) ((b - 128) * contrast + 128);

                r = Math.min(255, Math.max(0, r));
                g = Math.min(255, Math.max(0, g));
                b = Math.min(255, Math.max(0, b));

                result.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }
        return result;
    }

    private BufferedImage adjustSaturation(BufferedImage img, double factor) {
        BufferedImage result = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);

        for (int x = 0; x < img.getWidth(); x++) {
            for (int y = 0; y < img.getHeight(); y++) {
                int rgb = img.getRGB(x, y);

                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                double gray = (0.3 * r + 0.59 * g + 0.11 * b);

                r = (int) (gray + (r - gray) * factor);
                g = (int) (gray + (g - gray) * factor);
                b = (int) (gray + (b - gray) * factor);

                r = Math.min(255, Math.max(0, r));
                g = Math.min(255, Math.max(0, g));
                b = Math.min(255, Math.max(0, b));

                result.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }
        return result;
    }

    private BufferedImage applySharpen(BufferedImage img) {
        float[] sharpenKernel = {
            0, -1, 0,
            -1, 5, -1,
            0, -1, 0
        };

        BufferedImage result = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
        java.awt.image.Kernel kernel = new java.awt.image.Kernel(3, 3, sharpenKernel);
        ConvolveOp op = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
        op.filter(img, result);

        return result;
    }

}
