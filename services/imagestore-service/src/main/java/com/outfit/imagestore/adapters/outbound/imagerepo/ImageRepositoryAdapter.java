package com.outfit.imagestore.adapters.outbound.imagerepo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.outfit.imagestore.usecases.outbound.imagerepo.ImageRepository;

import jakarta.annotation.PostConstruct;

public class ImageRepositoryAdapter implements ImageRepository {

    private final ImageRepositoryConfig imageRepositoryConfig;
    private final AtomicInteger imageCounter;
    private final Path imagesDirectory;
    private final Pattern imageFilePattern;

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
            // Get next available ID
            int newImageId = imageCounter.incrementAndGet();

            // Determine file extension (default to jpg if not provided)
            String extension = determineExtension(originalExtension);

            // Create file path
            Path imagePath = imagesDirectory.resolve(newImageId + "." + extension);

            // Write image data
            Files.write(imagePath, imageData);

            System.out.println("Saved new image: " + imagePath.getFileName());
            return newImageId;

        } catch (IOException e) {
            System.err.println("Error saving image: " + e.getMessage());
            throw new RuntimeException("Failed to save image", e);
        }
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

    private String determineExtension(String originalExtension) {
        if (originalExtension == null || originalExtension.trim().isEmpty()) {
            return "jpg"; // default
        }

        String ext = originalExtension.toLowerCase().trim();
        if (ext.startsWith(".")) {
            ext = ext.substring(1);
        }

        // Validate extension
        if (ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png")) {
            return ext;
        }

        return "jpg"; // fallback to jpg for unsupported formats
    }

}
