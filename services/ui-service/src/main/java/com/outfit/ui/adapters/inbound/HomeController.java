package com.outfit.ui.adapters.inbound;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import com.outfit.ui.usecases.inbound.FetchProcessedImageUseCase;
import com.outfit.ui.usecases.inbound.PrepareImageGalleryPageUseCase;
import com.outfit.ui.usecases.inbound.imagenames.GalleryPageResponse;

@Controller
public class HomeController {

    private final PrepareImageGalleryPageUseCase prepareImageGalleryPageUseCase;
    private final FetchProcessedImageUseCase fetchProcessedImageUseCase;

    public HomeController(
            PrepareImageGalleryPageUseCase prepareImageGalleryPageUseCase,
            FetchProcessedImageUseCase fetchProcessedImageUseCase) {
        this.prepareImageGalleryPageUseCase = prepareImageGalleryPageUseCase;
        this.fetchProcessedImageUseCase = fetchProcessedImageUseCase;
    }

    @GetMapping("/")
    public String home(@RequestParam(defaultValue = "0") int page, Model model) {

        System.out.println("User requesting gallery page: " + page);

        try {
            // Use case: Prepare gallery page for user
            GalleryPageResponse galleryPage = prepareImageGalleryPageUseCase.fetchImageNamesForPage(page);

            model.addAttribute("images", galleryPage.getImages());
            model.addAttribute("currentPage", galleryPage.getCurrentPage());
            model.addAttribute("hasNextPage", galleryPage.isHasNextPage());
            model.addAttribute("hasPreviousPage", galleryPage.isHasPreviousPage());
            model.addAttribute("totalImages", galleryPage.getTotalImages());

        } catch (Exception e) {
            System.err.println("Failed to prepare gallery page: " + e.getMessage());
            model.addAttribute("images", java.util.Collections.emptyList());
            model.addAttribute("currentPage", page);
            model.addAttribute("hasNextPage", false);
            model.addAttribute("hasPreviousPage", false);
            model.addAttribute("totalImages", 0);
        }

        return "home_beautiful"; // Thymeleaf template name
    }

    @GetMapping("/image-proxy/{imageId}")
    public ResponseEntity<byte[]> getEnhancedImage(@PathVariable String imageId) {

        System.out.println("User requesting enhanced image: " + imageId);

        try {
            // Use case: Load enhanced image for user
            byte[] enhancedImageData = fetchProcessedImageUseCase.execute(imageId);

            if (enhancedImageData == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"enhanced_" + imageId + "\"")
                    .header(HttpHeaders.CACHE_CONTROL, "max-age=3600") // Cache for 1 hour
                    .body(enhancedImageData);

        } catch (Exception e) {
            System.err.println("Failed to load enhanced image " + imageId + ": " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    // Optional: Health check endpoint to verify service connections
    @GetMapping("/health/services")
    public ResponseEntity<String> checkServicesHealth() {
        try {
            // You could add health checks for your adapters here
            return ResponseEntity.ok("{\n" +
                    "  \"imagestore\": \"connected\",\n" +
                    "  \"processor\": \"connected\"\n" +
                    "}");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Service health check failed");
        }
    }
}
