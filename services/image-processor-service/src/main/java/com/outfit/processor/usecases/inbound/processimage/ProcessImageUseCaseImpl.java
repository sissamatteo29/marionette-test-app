package com.outfit.processor.usecases.inbound.processimage;

import org.springframework.stereotype.Component;

import com.outfit.processor.usecases.inbound.ProcessImageUseCase;
import com.outfit.processor.usecases.outbound.fetchimages.FetchImageGateway;

@Component
public class ProcessImageUseCaseImpl implements ProcessImageUseCase {

    private final FetchImageGateway fetchImageGateway;
    private final ImageProcessor imageProcessor;

    public ProcessImageUseCaseImpl(FetchImageGateway fetchImageGateway, ImageProcessor imageProcessor) {
        this.fetchImageGateway = fetchImageGateway;
        this.imageProcessor = imageProcessor;
    }

    @Override
    public byte[] execute(String imageName) {
        byte[] imageFromStore = fetchImageGateway.fetchImage(imageName);
        return imageProcessor.enhanceImage(imageFromStore);
    }
    
}
