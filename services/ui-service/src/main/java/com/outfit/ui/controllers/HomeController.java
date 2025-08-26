package com.outfit.ui.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.outfit.common.ProcessedImageResponse;

@Controller
public class HomeController {

    private final RestTemplate restTemplate = new RestTemplate();

    private final String imageProcessorServiceUrl;

    public HomeController(@Value("${image.processor.service.url}") String imageProcessorServiceUrl) {
        this.imageProcessorServiceUrl = imageProcessorServiceUrl;
    }

    @GetMapping("/")
    public String home(@RequestParam(defaultValue = "0") int page, Model model) {

        System.out.println("Received user request for a new page, starting...");

        try {
            // Call image processor microservice
            String url = imageProcessorServiceUrl + "?page=" + page;
            ResponseEntity<List<ProcessedImageResponse>> response
                    = restTemplate.exchange(
                            url,
                            org.springframework.http.HttpMethod.GET,
                            null,
                            new ParameterizedTypeReference<List<ProcessedImageResponse>>() {}
                    );

            List<ProcessedImageResponse> images = response.getBody();
            
            // IMPORTANT: Modify URLs to point to THIS service's proxy endpoint
            if (images != null) {
                images.forEach(img -> {
                    // Extract filename from original URL
                    String originalUrl = img.getUrl();
                    String filename = extractFilenameFromUrl(originalUrl);
                    
                    // Replace with proxy URL
                    img.setUrl("/image-proxy/" + filename);
                });
            }
            
            model.addAttribute("images", images != null ? images : List.of());
            model.addAttribute("currentPage", page);
            model.addAttribute("hasNextPage", images != null && !images.isEmpty());

        } catch (Exception e) {
            System.err.println("Failed to fetch images from processor service: " + e.getMessage());
            model.addAttribute("images", List.of());
            model.addAttribute("currentPage", page);
            model.addAttribute("hasNextPage", false);
        }

        return "home";
    }
    
    // NEW: Proxy endpoint that forwards image requests to the imagestore service
    @GetMapping("/image-proxy/{filename}")
    public ResponseEntity<byte[]> proxyImage(@PathVariable String filename) {
        
        System.out.println("Proxying image request for: " + filename);
        
        try {
            // Forward request to imagestore service (internal Kubernetes DNS)
            String imageUrl = imageProcessorServiceUrl + "/image/" + filename;
            
            ResponseEntity<byte[]> imageResponse = restTemplate.getForEntity(imageUrl, byte[].class);
            
            System.out.println("Successfully proxied image: " + filename + 
                             " (size: " + (imageResponse.getBody() != null ? imageResponse.getBody().length : 0) + " bytes)");
            
            // Forward the complete response back to the browser
            return ResponseEntity.status(imageResponse.getStatusCode())
                    .contentType(imageResponse.getHeaders().getContentType())
                    .headers(h -> h.addAll(imageResponse.getHeaders()))
                    .body(imageResponse.getBody());
                    
        } catch (RestClientException e) {
            System.err.println("Failed to proxy image " + filename + ": " + e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            System.err.println("Unexpected error proxying image " + filename + ": " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // Helper method to extract filename from URL
    private String extractFilenameFromUrl(String url) {
        if (url == null || url.isEmpty()) {
            return "";
        }
        
        // Handle URLs like "/images/photo.jpg" or "/image/photo.jpg"
        int lastSlash = url.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < url.length() - 1) {
            return url.substring(lastSlash + 1);
        }
        
        // Fallback: return the whole URL if no slash found
        return url;
    }
}
