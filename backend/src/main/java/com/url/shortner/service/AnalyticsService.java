package com.url.shortner.service;

import com.blueconic.browscap.Capabilities;
import com.blueconic.browscap.UserAgentParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.springframework.web.client.RestTemplate;
import java.util.Map;

@Service
public class AnalyticsService {
        @Autowired
        private RedisTemplate<String, Object> redisTemplate;

        @Autowired
        private UserAgentParser userAgentParser;

        @Async
        public void updateAnalytics(String shortCode, String ipAddress, String userAgent) {

                // 🔥 1. Total clicks
                redisTemplate.opsForValue().increment("click_total:" + shortCode);

                // 🔥 2. Daily clicks
                String today = LocalDate.now().toString();
                redisTemplate.opsForHash().increment(
                                "daily:" + shortCode,
                                today,
                                1);

                // 🔥 3. Hourly clicks
                String hourKey = LocalDateTime.now()
                                .truncatedTo(ChronoUnit.HOURS)
                                .toString();

                redisTemplate.opsForHash().increment(
                                "hourly:" + shortCode,
                                hourKey,
                                1);

                // 🔥 4. Geo clicks
                String location = "Unknown";
                try {
                        if (ipAddress != null && !ipAddress.equals("127.0.0.1") && !ipAddress.equals("0:0:0:0:0:0:0:1")
                                        && !ipAddress.startsWith("192.168.")) {
                                RestTemplate restTemplate = new RestTemplate();
                                String apiUrl = "http://ip-api.com/json/" + ipAddress;
                                @SuppressWarnings("unchecked")
                                Map<String, Object> response = restTemplate.getForObject(apiUrl, Map.class);
                                if (response != null && "success".equals(response.get("status"))) {
                                        String country = (String) response.get("country");
                                        String city = (String) response.get("city");
                                        String region = (String) response.get("regionName");

                                        StringBuilder locBuilder = new StringBuilder();
                                        if (city != null && !city.isEmpty()) {
                                                locBuilder.append(city).append(", ");
                                        }
                                        if (region != null && !region.isEmpty()) {
                                                locBuilder.append(region).append(", ");
                                        }
                                        if (country != null && !country.isEmpty()) {
                                                locBuilder.append(country);
                                        }

                                        if (locBuilder.length() > 0) {
                                                location = locBuilder.toString();
                                                // Handle case where country is missing and string ends with ", "
                                                if (location.endsWith(", ")) {
                                                        location = location.substring(0, location.length() - 2);
                                                }
                                        } else {
                                                location = "Unknown";
                                        }
                                }
                        } else {
                                location = "Localhost";
                        }
                } catch (Exception e) {
                        System.err.println("Error fetching geo location: " + e.getMessage());
                }

                if (location != null) {
                        redisTemplate.opsForHash().increment(
                                        "geo:" + shortCode,
                                        location,
                                        1);
                }

                // 🔥 5. Trending URLs (global)
                // redisTemplate.opsForZSet().incrementScore(
                // "trending_urls",
                // shortCode,
                // 1
                // );

                // 🔥 6. User Agent (OS, Device, Browser)
                try {
                        if (userAgent != null && !userAgent.trim().isEmpty() && !userAgent.equals("Unknown")) {
                                Capabilities capabilities = userAgentParser.parse(userAgent);

                                String os = capabilities.getPlatform();
                                String device = capabilities.getDeviceType();
                                String browser = capabilities.getBrowser();

                                if (os != null && !os.equals("Unknown")) {
                                        redisTemplate.opsForHash().increment("os:" + shortCode, os, 1);
                                }
                                if (device != null && !device.equals("Unknown")) {
                                        redisTemplate.opsForHash().increment("device:" + shortCode, device, 1);
                                }
                                if (browser != null && !browser.equals("Unknown")) {
                                        redisTemplate.opsForHash().increment("browser:" + shortCode, browser, 1);
                                }
                        }
                } catch (Exception e) {
                        System.err.println("Error parsing user agent: " + e.getMessage());
                }
        }
}
