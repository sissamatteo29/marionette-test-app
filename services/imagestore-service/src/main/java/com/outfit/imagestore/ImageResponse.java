package com.outfit.imagestore;

public class ImageResponse {

    String fileName;
    String base64Content;
    
    public ImageResponse(String fileName, String base64Content) {
        this.fileName = fileName;
        this.base64Content = base64Content;
    }

    public String getFileName() {
        return fileName;
    }

    public String getBase64Content() {
        return base64Content;
    }
    
}
