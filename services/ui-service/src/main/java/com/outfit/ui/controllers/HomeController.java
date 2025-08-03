package com.outfit.ui.controllers;

import java.util.List;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import com.outfit.common.ProcessedImageResponse;

@Controller
public class HomeController {

    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/")
    public String home(@RequestParam(defaultValue = "0") int page, Model model) {
        // Call image processor microservice (assuming it returns a List<String> of image URLs)
        String url = "http://localhost:8081/process?page=" + page;
        ResponseEntity<List<ProcessedImageResponse>> response
                = restTemplate.exchange(
                        url,
                        org.springframework.http.HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<List<ProcessedImageResponse>>() {
                }
                );

        List<ProcessedImageResponse> images = response.getBody();
        model.addAttribute("images", images);
        model.addAttribute("currentPage", page);
        model.addAttribute("hasNextPage", images != null && !images.isEmpty());

        return "home";
    }
}
