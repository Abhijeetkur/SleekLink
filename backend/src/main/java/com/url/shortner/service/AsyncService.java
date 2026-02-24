package com.url.shortner.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AsyncService {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Async
    public void incrementClick(String shortCode) {
        System.out.println("Incrementing: " + shortCode);
        redisTemplate.opsForValue().increment("click:" + shortCode);
    }
}
