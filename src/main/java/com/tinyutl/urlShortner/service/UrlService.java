package com.tinyutl.urlShortner.service;

import com.tinyutl.urlShortner.repository.UrlRepository;
import com.tinyutl.urlShortner.encoder.Base62Encoder;
import com.tinyutl.urlShortner.entity.UrlMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

import static org.yaml.snakeyaml.nodes.NodeId.mapping;

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
}
