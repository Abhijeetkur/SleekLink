package com.url.shortner.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api")
public class HealthController {

    private final Instant startTime = Instant.now();

    @Value("${spring.application.name:urlShortner}")
    private String appName;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        long uptimeSeconds = Instant.now().getEpochSecond() - startTime.getEpochSecond();

        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "application", appName,
                "timestamp", Instant.now().toString(),
                "uptime", formatUptime(uptimeSeconds)));
    }

    private String formatUptime(long totalSeconds) {
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (days > 0) {
            return String.format("%dd %dh %dm %ds", days, hours, minutes, seconds);
        } else if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        }
        return String.format("%ds", seconds);
    }
}
