package com.url.shortner.controller;

import com.url.shortner.service.UrlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

import org.springframework.web.bind.annotation.CrossOrigin;
import jakarta.servlet.http.HttpServletRequest;

@CrossOrigin(origins = "*")
@RestController
public class UrlController {

    @Autowired
    private UrlService urlService;

    @PostMapping("/shorten")
    public String shorten(@RequestBody Map<String, String> request, java.security.Principal principal) {
        String username = principal != null ? principal.getName() : null;
        return urlService.shortenUrl(request.get("longUrl"), username);
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<?> redirect(@PathVariable String shortCode,
            @RequestHeader(value = "User-Agent", defaultValue = "Unknown") String userAgent,
            HttpServletRequest request) {

        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        if (ipAddress != null && ipAddress.contains(",")) {
            ipAddress = ipAddress.split(",")[0].trim();
        }

        String longUrl = urlService.getLongUrl(shortCode, userAgent, ipAddress);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(longUrl))
                .build();
    }

    @GetMapping("/analytics/{shortCode}")
    public Map<String, Object> getAnalytics(@PathVariable String shortCode, java.security.Principal principal) {
        return urlService.getAnalytics(shortCode, principal.getName());
    }
}
