package com.outfit.imagestore.usecases.outbound.imagerepo;

public interface ImageRepository {

    public byte[] getImage(int imageId);

    public int putImage(byte[] imageData, String originalExtension);  // Returns the imageId
    
}
