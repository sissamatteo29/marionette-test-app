package com.outfit.imagestore.controllers;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.outfit.imagestore.ImageMetadata;

@RestController
public class ImageStoreController {

    private final Path imageDir = Paths.get("images"); // local folder
    private final Path cacheDir = Paths.get("cache");

    @GetMapping("/images")
    public ResponseEntity<List<ImageMetadata>> getImages(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "4") int size) throws IOException {

        if (!Files.exists(imageDir)) {
            return ResponseEntity.ok(List.of());
        }

        List<String> allFileNames = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(imageDir)) {
            for (Path entry : stream) {
                String name = entry.getFileName().toString().toLowerCase();
                if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg")) {
                    allFileNames.add(name);
                }
            }
        }

        // Apply pagination
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, allFileNames.size());

        if (fromIndex >= allFileNames.size()) {
            return ResponseEntity.ok(List.of()); // no images on this page
        }

        List<ImageMetadata> images = new ArrayList<>();
        for (String fileName : allFileNames.subList(fromIndex, toIndex)) {
            String url = "/images/" + fileName; // relative URL
            images.add(new ImageMetadata(fileName, url));
        }

        return ResponseEntity.ok(images);
    }

    @GetMapping("/image/{filename}")
    public ResponseEntity<byte[]> getCompressedThumbnail(@PathVariable("filename") String filename) throws IOException {
        Path cachedPath = cacheDir.resolve(filename);

        // If cached version exists, serve it directly
        if (Files.exists(cachedPath)) {
            byte[] cachedBytes = Files.readAllBytes(cachedPath);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(cachedBytes);
        }

        // Otherwise, load and process the original
        Path filePath = imageDir.resolve(filename).normalize();
        if (!Files.exists(filePath)) {
            return ResponseEntity.notFound().build();
        }

        BufferedImage original = ImageIO.read(filePath.toFile());
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

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .contentType(MediaType.IMAGE_JPEG)
                .body(compressedBytes);
    }



    
}
