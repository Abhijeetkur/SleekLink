package com.url.shortner.service;

import com.url.shortner.repository.UrlRepository;
import com.url.shortner.encoder.Base62Encoder;
import com.url.shortner.entity.UrlMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
public class UrlService {
    @Autowired
    private UrlRepository urlRepository;

    @Autowired
    private AsyncService asyncService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public String shortenUrl(String longUrl){
        UrlMapping entity = new UrlMapping();
        entity.setLongUrl(longUrl);
        entity = urlRepository.save(entity);

        String shortCode = Base62Encoder.encode(entity.getId());
        entity.setShortCode(shortCode);
        urlRepository.save(entity);

        redisTemplate.opsForValue().set(
                "url:" + shortCode,
                longUrl,
                Duration.ofHours(24)
        );

        redisTemplate.opsForValue().set("url:" + shortCode, longUrl);

        return shortCode;
    }

    public String getLongUrl(String shortCode) {
        String cachedUrl = (String) redisTemplate.opsForValue().get("url:" + shortCode);

        if (cachedUrl != null) {
            asyncService.incrementClick(shortCode); // still count click
            return cachedUrl; // ⚡ super fast
        }


        UrlMapping mapping = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new RuntimeException("URL not found"));

        String longUrl = mapping.getLongUrl();

        redisTemplate.opsForValue().set(
                "url:" + shortCode,
                longUrl,
                Duration.ofHours(24)
        );

        redisTemplate.opsForValue().set("url:" + shortCode, longUrl);

        asyncService.incrementClick(shortCode);

        return longUrl;
    }

    public Map<String, Object> getAnalytics(String shortCode) {

        UrlMapping mapping = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new RuntimeException("URL not found"));

        // DB clicks
        System.out.println("dbClicks");
        int dbClicks = mapping.getClickCount();

        // Redis clicks
        System.out.println("redisClicks");
        Object redisValue = redisTemplate.opsForValue().get("click:" + shortCode);
        int redisClicks = redisValue != null ? Integer.parseInt(redisValue.toString()) : 0;

        int totalClicks = dbClicks + redisClicks;

        Map<String, Object> response = new HashMap<>();
        response.put("shortCode", shortCode);
        response.put("totalClicks", totalClicks);

        return response;
    }
}
