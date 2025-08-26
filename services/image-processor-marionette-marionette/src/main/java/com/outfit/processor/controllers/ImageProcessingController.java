package com.outfit.processor.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import java.util.concurrent.Executors;
import java.awt.image.BufferedImage;
import org.springframework.http.ResponseEntity;
import java.io.ByteArrayOutputStream;
import java.awt.Graphics2D;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.concurrent.ExecutorService;
import java.awt.image.ConvolveOp;
import java.io.InputStream;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;
import com.outfit.common.ProcessedImageResponse;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;

@RestController
public class ImageProcessingController {

    private final RestTemplate restTemplate = new RestTemplate();

    // adjust port
    private final String imageStoreBaseUrl;
    private final String imageProcessorServiceUrl;

    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    public ImageProcessingController(
            @Value("${image.store.service.url}") String imageStoreBaseUrl,
            @Value("${image.processor.service.url}") String imageProcessorServiceUrl) {
        this.imageStoreBaseUrl = imageStoreBaseUrl;
        this.imageProcessorServiceUrl = imageProcessorServiceUrl;
    }

    @GetMapping("/image/{fileName}")
    public ResponseEntity<byte[]> getProcessedImage(@PathVariable String fileName) {

        System.out.println("Received processing request for image " + fileName);

        try {
            byte[] processedBytes = processImageBytes(fileName);
            return ResponseEntity.ok().header("Content-Type", "image/jpeg").body(processedBytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private byte[] processImageBytes(String fileName) {
        switch(com.outfit.processor.__marionette.BehaviourRegistry.getBehaviourId("com/outfit/processor/controllers/ImageProcessingController.java", "processImageBytes")) {
            default:
            case "default":
                return processImageBytes_default(fileName);
            case "low_energy":
                return processImageBytes_low_energy(fileName);
        }
    }

    @GetMapping
    public ResponseEntity<List<ProcessedImageResponse>> processImages(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "4") int size) {

        System.out.println("Received request for processing image page number " + page);


        // 1. Fetch image metadata (list of filenames)
        String metadataUrl = imageStoreBaseUrl + "/images?page=" + page + "&size=" + size;
        List<Map<String, String>> metadata = restTemplate.getForObject(metadataUrl, List.class);
        if (metadata == null || metadata.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }
        List<ProcessedImageResponse> results = metadata.stream().map(img -> {
            String fileName = img.get("fileName");
            // URL that triggers on-demand processing
            String url = imageProcessorServiceUrl + "/process/image/" + fileName;
            return new ProcessedImageResponse(fileName, url);
        }).collect(Collectors.toList());
        return ResponseEntity.ok(results);
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
        float[] sharpenKernel = { 0, -1, 0, -1, 5, -1, 0, -1, 0 };
        BufferedImage result = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
        java.awt.image.Kernel kernel = new java.awt.image.Kernel(3, 3, sharpenKernel);
        ConvolveOp op = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
        op.filter(img, result);
        return result;
    }

    private byte[] processImageBytes_default(String fileName) {

        System.out.println("Received request and starting default method to process images");

        try {
            String imageUrl = imageStoreBaseUrl + "/image/" + fileName;
            Resource resource = restTemplate.getForObject(imageUrl, Resource.class);
            if (resource == null) {
                throw new RuntimeException("Could not fetch image: " + fileName);
            }
            try (InputStream in = resource.getInputStream();
                ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                BufferedImage image = ImageIO.read(in);
                BufferedImage adjusted = adjustBrightnessContrast(image, 1.1, 1);
                BufferedImage saturated = adjustSaturation(adjusted, 1.2);
                BufferedImage sharpened = applySharpen(saturated);
                ImageIO.write(sharpened, "jpeg", baos);
                return baos.toByteArray();
            }
        } catch (Exception e) {
            throw new RuntimeException("Error processing image " + fileName, e);
        }
    }

    private byte[] processImageBytes_low_energy(String fileName) {
        try {
            String imageUrl = imageStoreBaseUrl + "/image/" + fileName;
            Resource resource = restTemplate.getForObject(imageUrl, Resource.class);
            if (resource == null) {
                throw new RuntimeException("Could not fetch image: " + fileName);
            }
            try (InputStream in = resource.getInputStream();
                ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                // Read the original image
                BufferedImage original = ImageIO.read(in);
                if (original == null) {
                    throw new RuntimeException("Invalid image format: " + fileName);
                }
                // 1. Rescale to half size
                int newWidth = original.getWidth() / 2;
                int newHeight = original.getHeight() / 2;
                BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = resized.createGraphics();
                g.drawImage(original, 0, 0, newWidth, newHeight, null);
                g.dispose();
                // 2. Convert to grayscale
                BufferedImage grayImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_BYTE_GRAY);
                Graphics2D g2 = grayImage.createGraphics();
                g2.drawImage(resized, 0, 0, null);
                g2.dispose();
                // Write the grayscale image to byte array
                ImageIO.write(grayImage, "jpeg", baos);
                return baos.toByteArray();
            }
        } catch (Exception e) {
            throw new RuntimeException("Error processing image " + fileName, e);
        }
    }
}
