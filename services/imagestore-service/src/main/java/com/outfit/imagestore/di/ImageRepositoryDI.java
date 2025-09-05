package com.outfit.imagestore.di;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.outfit.imagestore.adapters.outbound.imagerepo.ImageRepositoryAdapter;
import com.outfit.imagestore.adapters.outbound.imagerepo.ImageRepositoryConfig;
import com.outfit.imagestore.usecases.outbound.imagerepo.ImageRepository;

@Configuration
public class ImageRepositoryDI {

    @Bean
    public ImageRepository imageRepository(ImageRepositoryConfig config) {
        return new ImageRepositoryAdapter(config);
    }
    
}
