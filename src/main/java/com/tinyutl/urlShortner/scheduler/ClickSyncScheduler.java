package com.tinyutl.urlShortner.scheduler;

import com.tinyutl.urlShortner.repository.UrlRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class ClickSyncScheduler {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private UrlRepository urlRepository;

    @Scheduled(fixedRate = 60000) // every 1 min
    public void syncClicks() {

        Set<String> keys = redisTemplate.keys("click:*");

        for (String key : keys) {
            String shortCode = key.replace("click:", "");
            Object redisVal = redisTemplate.opsForValue().get(key);

            if (redisVal != null) {
                Integer count = Integer.parseInt(redisVal.toString());
                urlRepository.incrementClickCount(shortCode, count);
                redisTemplate.delete(key);
            }
        }
    }
}
