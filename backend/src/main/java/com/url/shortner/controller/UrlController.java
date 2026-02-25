package com.url.shortner.controller;

import com.url.shortner.service.UrlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

import org.springframework.web.bind.annotation.CrossOrigin;

@CrossOrigin(origins = "*")
@RestController
public class UrlController {

    @Autowired
    private UrlService urlService;

    @PostMapping("/shorten")
    public String shorten(@RequestBody Map<String, String> request) {
        return urlService.shortenUrl(request.get("longUrl"));
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<?> redirect(@PathVariable String shortCode,
            @RequestHeader(value = "User-Agent", defaultValue = "Unknown") String userAgent) {
        String longUrl = urlService.getLongUrl(shortCode, userAgent);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(longUrl))
                .build();
    }

    @GetMapping("/analytics/{shortCode}")
    public Map<String, Object> getAnalytics(@PathVariable String shortCode) {
        return urlService.getAnalytics(shortCode);
    }
}
