package com.outfit.processor.controllers.marionette;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.springframework.core.io.Resource;
import org.springframework.web.client.RestTemplate;

public class ImageProcessingController_Marionette_SuperLowen {

    private final RestTemplate restTemplate = new RestTemplate();
    private String imageStoreBaseUrl;

    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    private byte[] processImageBytes(String fileName) {
        try {
            // Fetch image
            String imageUrl = imageStoreBaseUrl + "/image/" + fileName;
            Resource resource = restTemplate.getForObject(imageUrl, Resource.class);
            if (resource == null) {
                throw new RuntimeException("Could not fetch image: " + fileName);
            }

            try (InputStream in = resource.getInputStream(); 
                 ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

                BufferedImage original = ImageIO.read(in);
                if (original == null) {
                    throw new RuntimeException("Invalid image format: " + fileName);
                }

                // 1. Aggressive resize - max 150px, min 25% of original
                int w = original.getWidth();
                int h = original.getHeight();
                double scale = Math.min(Math.min(150.0 / w, 150.0 / h), 0.25);
                int newW = Math.max(1, (int) (w * scale));
                int newH = Math.max(1, (int) (h * scale));

                BufferedImage resized = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
                Graphics2D graphics = resized.createGraphics();
                graphics.drawImage(original, 0, 0, newW, newH, null);
                graphics.dispose();

                // 2. Color quantization - reduce to 16 color levels per channel
                for (int y = 0; y < newH; y++) {
                    for (int x = 0; x < newW; x++) {
                        int rgb = resized.getRGB(x, y);
                        int r = ((rgb >> 16) & 0xFF) / 16 * 16;
                        int green = ((rgb >> 8) & 0xFF) / 16 * 16;
                        int b = (rgb & 0xFF) / 16 * 16;
                        resized.setRGB(x, y, (r << 16) | (green << 8) | b);
                    }
                }

                // 3. Convert to grayscale
                BufferedImage gray = new BufferedImage(newW, newH, BufferedImage.TYPE_BYTE_GRAY);
                Graphics2D g2 = gray.createGraphics();
                g2.drawImage(resized, 0, 0, null);
                g2.dispose();

                // 4. Ultra-low quality JPEG compression
                ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
                ImageWriteParam param = writer.getDefaultWriteParam();
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(0.1f); // 10% quality

                ImageOutputStream ios = ImageIO.createImageOutputStream(baos);
                writer.setOutput(ios);
                writer.write(null, new javax.imageio.IIOImage(gray, null, null), param);
                
                writer.dispose();
                ios.close();

                return baos.toByteArray();
            }

        } catch (Exception e) {
            throw new RuntimeException("Error processing image " + fileName, e);
        }
    }
    
}
