package com.url.shortner.service;

import com.blueconic.browscap.Capabilities;
import com.blueconic.browscap.UserAgentParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsService {

        @Autowired
        private KafkaProducerService kafkaProducerService;

        @Autowired
        private UserAgentParser userAgentParser;

        public void updateAnalytics(String shortCode, String country, String device) {
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
                kafkaProducerService.sendClickEvent(shortCode, country, os, browser, deviceType);
        }
}
