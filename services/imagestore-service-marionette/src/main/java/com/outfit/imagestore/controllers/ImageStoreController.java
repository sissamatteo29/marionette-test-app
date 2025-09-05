package com.outfit.imagestore.controllers;

import javax.imageio.ImageWriter;
import javax.imageio.ImageWriteParam;
import java.awt.image.BufferedImage;
import org.springframework.http.ResponseEntity;
import java.util.ArrayList;
import java.io.ByteArrayOutputStream;
import javax.imageio.stream.ImageOutputStream;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.awt.Graphics2D;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.Objects;
import java.nio.file.Path;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.stream.Collectors;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;
import com.outfit.imagestore.ImageMetadata;
import javax.imageio.ImageIO;
import java.nio.file.Files;
import org.springframework.http.HttpHeaders;
import javax.imageio.IIOImage;
import java.nio.file.Paths;
import java.awt.Image;
import java.util.Optional;
import java.util.Arrays;
import org.springframework.http.MediaType;

@RestController
public class ImageStoreController {

    private final ResourceLoader resourceLoader;

    Path cacheDir = Paths.get("/app/cache");

    public ImageStoreController(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    public void init() throws IOException {
        // Ensure cache directory exists
        if (!Files.exists(cacheDir)) {
            Files.createDirectories(cacheDir);
            System.out.println("Created cache directory: " + cacheDir.toAbsolutePath());
        } else {
            System.out.println("Cache directory already exists: " + cacheDir.toAbsolutePath());
        }
    }

    @GetMapping("/images")
    public ResponseEntity<List<ImageMetadata>> getImages(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "4") int size) throws IOException {
        System.out.println("Received request for image links on page " + page);
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath:static/images/*.{jpg,jpeg,png}");
        List<String> allFileNames = Arrays.stream(resources).map(resource -> resource.getFilename()).filter(Objects::nonNull).collect(Collectors.toList());
        System.out.println("Retrieved " + allFileNames.size() + " file names from local storage");
        // Apply pagination
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, allFileNames.size());
        if (fromIndex >= allFileNames.size()) {
            // no images on this page
            return ResponseEntity.ok(List.of());
        }
        List<ImageMetadata> images = new ArrayList<>();
        for (String fileName : allFileNames.subList(fromIndex, toIndex)) {
            // relative URL
            String url = "/images/" + fileName;
            images.add(new ImageMetadata(fileName, url));
        }
        System.out.println("Returning image names");
        return ResponseEntity.ok(images);
    }

    @GetMapping("/image/{filename}")
    public ResponseEntity<byte[]> getCompressedThumbnail(@PathVariable("filename") String filename) throws IOException {
        System.out.println("Received a request for the image " + filename);
        Path cachedPath = cacheDir.resolve(filename);
        Optional<byte[]> cachedImage = getFromCache(cachedPath);
        if (cachedImage.isPresent()) {
            return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"").contentType(MediaType.IMAGE_JPEG).body(cachedImage.get());
        }
        System.out.println("The file did not exist in the cache, loading it from static resources");
        // Otherwise, load and process the original
        Resource imageResource = resourceLoader.getResource("classpath:static/images/" + filename);
        if (!imageResource.exists()) {
            System.out.println("Impossible to find the file on the classpath, retunring nothing");
            return ResponseEntity.notFound().build();
        }
        BufferedImage original = ImageIO.read(imageResource.getInputStream());
        if (original == null) {
            return ResponseEntity.badRequest().build();
        }
        // Resize to medium (600px width)
        int targetWidth = 800;
        int targetHeight = (original.getHeight() * targetWidth) / original.getWidth();
        Image scaled = original.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
        BufferedImage resized = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        g.drawImage(scaled, 0, 0, null);
        g.dispose();
        // Compress to JPEG (~80% quality)
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageWriter jpgWriter = ImageIO.getImageWritersByFormatName("jpeg").next();
        ImageWriteParam jpgWriteParam = jpgWriter.getDefaultWriteParam();
        jpgWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        jpgWriteParam.setCompressionQuality(0.8f);
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
            jpgWriter.setOutput(ios);
            jpgWriter.write(null, new IIOImage(resized, null, null), jpgWriteParam);
        }
        jpgWriter.dispose();
        byte[] compressedBytes = baos.toByteArray();
        // Save to cache for next time
        Files.write(cachedPath, compressedBytes);
        System.out.println("Managed to apply compression, sending the image " + filename + " back to client...");
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"").contentType(MediaType.IMAGE_JPEG).body(compressedBytes);
    }

    private Optional<byte[]> getFromCache(Path pathToFileInCache) throws IOException {
        switch(com.outfit.imagestore.__marionette.BehaviourRegistry.getBehaviourId("com/outfit/imagestore/controllers/ImageStoreController.java", "getFromCache")) {
            case "noCache":
                return getFromCache_noCache(pathToFileInCache);
            default:
            case "useCache":
                return getFromCache_useCache(pathToFileInCache);
        }
    }

    private Optional<byte[]> getFromCache_noCache(Path filename) throws IOException {
        return Optional.empty();
    }

    private Optional<byte[]> getFromCache_useCache(Path pathToFileInCache) throws IOException {
        // If cached version exists, serve it directly
        if (Files.exists(pathToFileInCache)) {
            byte[] cachedBytes = Files.readAllBytes(pathToFileInCache);
            return Optional.of(cachedBytes);
        } else {
            return Optional.empty();
        }
    }
}
