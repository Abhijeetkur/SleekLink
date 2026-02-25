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

@Service
public class AnalyticsService {
        @Autowired
        private RedisTemplate<String, Object> redisTemplate;

        @Autowired
        private UserAgentParser userAgentParser;

        @Async
        public void updateAnalytics(String shortCode, String country, String userAgent) {

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
                // redisTemplate.opsForHash().increment(
                // "geo:" + shortCode,
                // country,
                // 1
                // );

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
