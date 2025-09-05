package com.outfit.ui.adapters.outbound.imagelist;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ImageStoreServiceFetchImageListAdapterConfig {

    @Value("${imagestore.service.url:http://localhost:8082}")
    private final String imageStoreServiceUrl;

    public String getImageStoreServiceUrl() {
        return imageStoreServiceUrl;
    }

    public ImageStoreServiceFetchImageListAdapterConfig(String imageStoreServiceUrl) {
        this.imageStoreServiceUrl = imageStoreServiceUrl;
    }
    
}
