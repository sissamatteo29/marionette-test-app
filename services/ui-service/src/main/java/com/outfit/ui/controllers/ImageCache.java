package com.outfit.ui.controllers;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ImageCache {

    private final Map<String, byte[]> imageCache = new HashMap<>();

    public Optional<byte[]> getImage(String imageName) {
        if(!imageCache.containsKey(imageName)) {
            return Optional.empty();
        } else {
            return Optional.of(imageCache.get(imageName));
        }
    }

    public void putImage(String imageName, byte[] image) {
        imageCache.put(imageName, image);
    }
    
}
