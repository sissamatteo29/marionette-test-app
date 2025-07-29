package com.outfit.processor.controllers;

public class ProcessedImageResponse {
    private final String fileName;
    private final String base64Content;

    public ProcessedImageResponse(String fileName, String base64Content) {
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
