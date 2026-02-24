package com.url.shortner.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class AnalyticsService {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public void updateAnalytics(String shortCode, String country) {

        // 🔥 1. Total clicks
        redisTemplate.opsForValue().increment("click_total:" + shortCode);

        // 🔥 2. Daily clicks
        String today = LocalDate.now().toString();
        redisTemplate.opsForHash().increment(
                "daily:" + shortCode,
                today,
                1
        );

        // 🔥 3. Hourly clicks
        String hourKey = LocalDateTime.now()
                .truncatedTo(ChronoUnit.HOURS)
                .toString();

        redisTemplate.opsForHash().increment(
                "hourly:" + shortCode,
                hourKey,
                1
        );

        // 🔥 4. Geo clicks
//        redisTemplate.opsForHash().increment(
//                "geo:" + shortCode,
//                country,
//                1
//        );

        // 🔥 5. Trending URLs (global)
//        redisTemplate.opsForZSet().incrementScore(
//                "trending_urls",
//                shortCode,
//                1
//        );
    }
}
