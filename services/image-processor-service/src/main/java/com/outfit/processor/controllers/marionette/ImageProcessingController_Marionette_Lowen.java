package com.outfit.processor.controllers.marionette;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.imageio.ImageIO;

import org.springframework.core.io.Resource;
import org.springframework.web.client.RestTemplate;

public class ImageProcessingController_Marionette_Lowen {

    private final RestTemplate restTemplate = new RestTemplate();
    private String imageStoreBaseUrl;

    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    private byte[] processImageBytes(String fileName) {
        try {
            String imageUrl = imageStoreBaseUrl + "/image/" + fileName;
            Resource resource = restTemplate.getForObject(imageUrl, Resource.class);

            if (resource == null) {
                throw new RuntimeException("Could not fetch image: " + fileName);
            }

            try (InputStream in = resource.getInputStream(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

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
