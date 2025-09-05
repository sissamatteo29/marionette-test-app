package com.outfit.imagestore.adapters.outbound.imagerepo;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import com.outfit.imagestore.usecases.outbound.imagerepo.ImageRepository;

import jakarta.annotation.PostConstruct;

public class ImageRepositoryAdapter implements ImageRepository {

    private final ImageRepositoryConfig imageRepositoryConfig;
    private final AtomicInteger imageCounter;
    private final Path imagesDirectory;
    private final Pattern imageFilePattern;

    // Configuration for auto-resizing
    private static final int MAX_WIDTH = 800; // Max width in pixels
    private static final int MAX_HEIGHT = 800; // Max height in pixels
    private static final float JPEG_QUALITY = 0.80f; // 80% quality
    private static final long MAX_FILE_SIZE = 500 * 1024; // 500KB target size

    public ImageRepositoryAdapter(ImageRepositoryConfig imageRepositoryConfig) {
        this.imageRepositoryConfig = imageRepositoryConfig;
        this.imageCounter = new AtomicInteger(0);
        this.imagesDirectory = Paths.get(imageRepositoryConfig.getImageStoragePath());
        // Pattern to match files like: 1.jpg, 123.png, 456.jpeg
        this.imageFilePattern = Pattern.compile("^(\\d+)\\.(jpg|jpeg|png)$", Pattern.CASE_INSENSITIVE);
    }

    @PostConstruct
    public void initializeCounter() throws IOException {
        // Ensure the images directory exists
        if (!Files.exists(imagesDirectory)) {
            Files.createDirectories(imagesDirectory);
            System.out.println("Created images directory: " + imagesDirectory.toAbsolutePath());
        }

        // Scan existing files to find the highest counter
        int maxCounter = scanForHighestCounter();
        imageCounter.set(maxCounter);

        System.out.println("Initialized image counter to: " + maxCounter +
                " (found " + countExistingImages() + " existing images)");
    }

    private int scanForHighestCounter() throws IOException {
        try (Stream<Path> files = Files.list(imagesDirectory)) {
            return files
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .map(imageFilePattern::matcher)
                    .filter(matcher -> matcher.matches())
                    .mapToInt(matcher -> Integer.parseInt(matcher.group(1)))
                    .max()
                    .orElse(0); // Start from 0 if no images found
        }
    }

    public List<String> getAllImages() throws IOException {
        try (Stream<Path> files = Files.list(imagesDirectory)) {
            return files
                    .filter(Files::isRegularFile) // Only files, not directories
                    .map(path -> path.getFileName().toString())
                    .filter(fileName -> imageFilePattern.matcher(fileName).matches())
                    .toList();
        }
    }

    private long countExistingImages() throws IOException {
        try (Stream<Path> files = Files.list(imagesDirectory)) {
            return files
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(fileName -> imageFilePattern.matcher(fileName).matches())
                    .count();
        }
    }

    @Override
    public byte[] getImage(String imageName) {
        try {

            Path imagePath = imagesDirectory.resolve(imageName);
            if (Files.exists(imagePath)) {
                System.out.println("Found image: " + imagePath.getFileName());
                return Files.readAllBytes(imagePath);
            }

            System.out.println("Image not found for name: " + imageName);
            return null; // or throw ImageNotFoundException

        } catch (IOException e) {
            System.err.println("Error reading image " + imageName + ": " + e.getMessage());
            throw new RuntimeException("Failed to read image: " + imageName, e);
        }
    }

    @Override
    public int putImage(byte[] imageData, String originalExtension) {
        try {
            // Process the image (resize if needed, compress)
            byte[] processedImageData = processImage(imageData);

            // Get next available ID
            int newImageId = imageCounter.incrementAndGet();

            // Always save as JPEG after processing
            Path imagePath = imagesDirectory.resolve(newImageId + ".jpg");

            // Write processed image data
            Files.write(imagePath, processedImageData);

            System.out.println("Saved image " + newImageId + ".jpg - " +
                    "Original size: " + formatFileSize(imageData.length) +
                    ", Processed size: " + formatFileSize(processedImageData.length) +
                    " (" + calculateCompressionRatio(imageData.length, processedImageData.length) + "% reduction)");

            return newImageId;

        } catch (IOException e) {
            System.err.println("Error saving image: " + e.getMessage());
            throw new RuntimeException("Failed to save image", e);
        }
    }

    private byte[] processImage(byte[] originalImageData) throws IOException {
        // Load the original image
        BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(originalImageData));
        if (originalImage == null) {
            throw new IllegalArgumentException("Invalid image format");
        }

        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        System.out.println("Original image dimensions: " + originalWidth + "x" + originalHeight);

        // Check if resizing is needed
        BufferedImage processedImage;
        if (needsResizing(originalWidth, originalHeight)) {
            processedImage = resizeImage(originalImage);
            System.out.println("Resized to: " + processedImage.getWidth() + "x" + processedImage.getHeight());
        } else {
            processedImage = originalImage;
            System.out.println("No resizing needed");
        }

        // Compress to JPEG with specified quality
        return compressToJpeg(processedImage);
    }

    private boolean needsResizing(int width, int height) {
        return width > MAX_WIDTH || height > MAX_HEIGHT;
    }

    private BufferedImage resizeImage(BufferedImage originalImage) {
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        // Calculate new dimensions while maintaining aspect ratio
        double widthRatio = (double) MAX_WIDTH / originalWidth;
        double heightRatio = (double) MAX_HEIGHT / originalHeight;
        double ratio = Math.min(widthRatio, heightRatio); // Use the smaller ratio to fit within bounds

        int newWidth = (int) (originalWidth * ratio);
        int newHeight = (int) (originalHeight * ratio);

        // Create resized image with high quality rendering
        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = resizedImage.createGraphics();

        // Set high quality rendering hints
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        graphics.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        graphics.dispose();

        return resizedImage;
    }

    private byte[] compressToJpeg(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        ImageWriter jpegWriter = ImageIO.getImageWritersByFormatName("jpeg").next();
        ImageWriteParam jpegParams = jpegWriter.getDefaultWriteParam();
        jpegParams.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        jpegParams.setCompressionQuality(JPEG_QUALITY);

        try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
            jpegWriter.setOutput(ios);
            jpegWriter.write(null, new IIOImage(image, null, null), jpegParams);
        }

        jpegWriter.dispose();
        return baos.toByteArray();
    }

    public int getCurrentCounter() {
        return imageCounter.get();
    }

    public long getTotalImages() {
        try {
            return countExistingImages();
        } catch (IOException e) {
            System.err.println("Error counting images: " + e.getMessage());
            return -1;
        }
    }

    public boolean imageExists(int imageId) {
        String[] extensions = { "jpg", "jpeg", "png" };

        for (String extension : extensions) {
            Path imagePath = imagesDirectory.resolve(imageId + "." + extension);
            if (Files.exists(imagePath)) {
                return true;
            }
        }
        return false;
    }

    public boolean deleteImage(int imageId) {
        try {
            String[] extensions = { "jpg", "jpeg", "png" };

            for (String extension : extensions) {
                Path imagePath = imagesDirectory.resolve(imageId + "." + extension);
                if (Files.exists(imagePath)) {
                    Files.delete(imagePath);
                    System.out.println("Deleted image: " + imagePath.getFileName());
                    return true;
                }
            }

            System.out.println("Image not found for deletion: " + imageId);
            return false;

        } catch (IOException e) {
            System.err.println("Error deleting image " + imageId + ": " + e.getMessage());
            throw new RuntimeException("Failed to delete image: " + imageId, e);
        }
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024)
            return bytes + " B";
        if (bytes < 1024 * 1024)
            return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    private int calculateCompressionRatio(long originalSize, long compressedSize) {
        if (originalSize == 0)
            return 0;
        return (int) ((originalSize - compressedSize) * 100 / originalSize);
    }

}
