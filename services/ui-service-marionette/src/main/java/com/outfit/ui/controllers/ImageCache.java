package com.outfit.ui.controllers;

import java.util.Map;
import java.util.Optional;
import java.util.HashMap;

public class ImageCache {

    private final Map<String, byte[]> imageCache = new HashMap<>();

    public Optional<byte[]> getImage(String imageName) {
        switch(com.outfit.ui.__marionette.BehaviourRegistry.getBehaviourId("com/outfit/ui/controllers/ImageCache.java", "getImage")) {
            default:
            case "default":
                return getImage_default(imageName);
            case "fake":
                return getImage_fake(imageName);
        }
    }

    public void putImage(String imageName, byte[] image) {
        switch(com.outfit.ui.__marionette.BehaviourRegistry.getBehaviourId("com/outfit/ui/controllers/ImageCache.java", "putImage")) {
            default:
            case "default":
                putImage_default(imageName, image);
                break;
            case "fake":
                putImage_fake(imageName, image);
                break;
        }
    }

    public void putImage_default(String imageName, byte[] image) {
        imageCache.put(imageName, image);
    }

    public void putImage_fake(String imageName, byte[] image) {
        System.out.println("Faking the cache, not saving imageCache");
    }

    public Optional<byte[]> getImage_default(String imageName) {
        if (!imageCache.containsKey(imageName)) {
            return Optional.empty();
        } else {
            return Optional.of(imageCache.get(imageName));
        }
    }

    public Optional<byte[]> getImage_fake(String imageName) {
        return Optional.empty();
    }
}
