package com.url.shortner.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class KafkaProducerService {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    private static final String TOPIC = "click-events";

    public void sendClickEvent(String shortCode, String country, String os, String browser, String deviceType) {
        String message = shortCode + "|" + country + "|" + os + "|" + browser + "|" + deviceType + "|"
                + LocalDateTime.now();
        kafkaTemplate.send(TOPIC, message);
    }
}
