package com.outfit.imagestore.adapters.controllers.marionette;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ImageStoreController_Marionette_NoCache {

    private final ResourceLoader resourceLoader;
    Path cacheDir = Paths.get("/app/cache");
    
    public ImageStoreController_Marionette_NoCache(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }


    private Optional<byte[]> getFromCache(Path filename) throws IOException {
        return Optional.empty();
    }

    
}
