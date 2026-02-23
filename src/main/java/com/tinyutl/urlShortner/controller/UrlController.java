package com.tinyutl.urlShortner.controller;

import com.tinyutl.urlShortner.service.UrlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

@RestController
public class UrlController {

    @Autowired
    private UrlService service;

    @PostMapping("/shorten")
    public String shorten(@RequestBody Map<String, String> request) {
        return service.shortenUrl(request.get("longUrl"));
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<?> redirect(@PathVariable String shortCode) {
        String longUrl = service.getLongUrl(shortCode);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(longUrl))
                .build();
    }
}
