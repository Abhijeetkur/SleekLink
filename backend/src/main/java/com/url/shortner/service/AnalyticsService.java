package com.url.shortner.service;

import com.blueconic.browscap.Capabilities;
import com.blueconic.browscap.UserAgentParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;

@Service
public class AnalyticsService {

        @Autowired
        private RedisTemplate<String, Object> redisTemplate;

        @Autowired
        private KafkaProducerService kafkaProducerService;

        @Autowired
        private UserAgentParser userAgentParser;

        public void updateAnalytics(String shortCode, String ipAddress, String device) {

                // DEBOUNCE LOGIC (Solves the Smartphone double-click / prefetch bug)
                // If this IP has clicked this shortCode in the last 5 seconds, ignore the
                // duplicate!
                String debounceKey = "debounce:" + shortCode + ":" + ipAddress;
                if (Boolean.TRUE.equals(redisTemplate.hasKey(debounceKey))) {
                        System.out.println("Ignored duplicate prefetch/click from IP: " + ipAddress);
                        return;
                }
                redisTemplate.opsForValue().set(debounceKey, "true", Duration.ofSeconds(5));

                String os = "Unknown";
                String browser = "Unknown";
                String deviceType = "Unknown";

                if (device != null && !device.isEmpty()) {
                        Capabilities capabilities = userAgentParser.parse(device);
                        os = capabilities.getPlatform() != null ? capabilities.getPlatform() : "Unknown";
                        browser = capabilities.getBrowser() != null ? capabilities.getBrowser() : "Unknown";
                        deviceType = capabilities.getDeviceType() != null ? capabilities.getDeviceType() : "Unknown";
                }

                // CQRS: Write-Side (Event Stream)
                // We ONLY send the event to Kafka. The consumer will handle DB and Cache
                // updates.
                kafkaProducerService.sendClickEvent(shortCode, ipAddress, os, browser, deviceType);
        }
}
