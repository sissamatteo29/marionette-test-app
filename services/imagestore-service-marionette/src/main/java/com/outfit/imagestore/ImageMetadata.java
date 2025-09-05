package com.outfit.imagestore;

public class ImageMetadata {
    private String fileName;
    private String url;

    public ImageMetadata(String fileName, String url) {
        this.fileName = fileName;
        this.url = url;
    }

    public String getFileName() {
        return fileName;
    }

    public String getUrl() {
        return url;
    }
}
