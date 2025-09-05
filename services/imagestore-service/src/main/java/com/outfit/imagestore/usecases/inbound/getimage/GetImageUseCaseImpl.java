package com.outfit.imagestore.usecases.inbound.getimage;

import org.springframework.stereotype.Component;

import com.outfit.imagestore.usecases.inbound.GetImageUseCase;
import com.outfit.imagestore.usecases.outbound.imagerepo.ImageRepository;

@Component
public class GetImageUseCaseImpl implements GetImageUseCase {

    private final ImageRepository imageRepository;

    public GetImageUseCaseImpl(ImageRepository imageRepository) {
        this.imageRepository = imageRepository;
    }

    @Override
    public byte[] execute(int imageId) {
        return imageRepository.getImage(imageId);
    }
    
}
