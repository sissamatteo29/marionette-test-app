package com.outfit.processor.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.outfit.common.ProcessedImageResponse;

@RestController
public class RootController {

    @Autowired
    private ImageProcessingController imageProcessingController;

    @GetMapping("/")
    public ResponseEntity<List<ProcessedImageResponse>> processImages(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "4") int size) {
        return imageProcessingController.processImages(page, size);
    }

    @GetMapping("/image/{fileName}")
    public ResponseEntity<byte[]> getProcessedImage(@PathVariable String fileName) {
        return imageProcessingController.getProcessedImage(fileName);
    }
}
