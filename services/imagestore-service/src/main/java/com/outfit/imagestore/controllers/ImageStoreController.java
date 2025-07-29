package com.outfit.imagestore.controllers;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
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
    public ResponseEntity<Resource> getImage(@PathVariable("filename") String filename) throws IOException {
        Path filePath = imageDir.resolve(filename).normalize();
        Resource resource = new UrlResource(filePath.toUri());

        if (!resource.exists() || !resource.isReadable()) {
            System.out.println("Requested path for image " + filePath);
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
            .contentType(MediaType.IMAGE_JPEG) // or detect dynamically
            .body(resource);

    }



    
}
