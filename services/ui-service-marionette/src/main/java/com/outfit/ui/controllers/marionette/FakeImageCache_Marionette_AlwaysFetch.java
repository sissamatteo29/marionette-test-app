package com.outfit.ui.controllers.marionette;

import java.util.Optional;

public class FakeImageCache_Marionette_AlwaysFetch {

    public Optional<byte[]> getImage(String imageName) {
        return Optional.empty();
    }

    public void putImage(String imageName, byte[] image) {
        System.out.println("Faking the cache, not saving imageCache");
    }
}
