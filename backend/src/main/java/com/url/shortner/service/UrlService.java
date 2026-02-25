package com.url.shortner.service;

import com.url.shortner.entity.DailyAnalytics;
import com.url.shortner.entity.GeoAnalytics;
import com.url.shortner.entity.HourlyAnalytics;
import com.url.shortner.repository.UrlRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.url.shortner.encoder.Base62Encoder;
import com.url.shortner.entity.UrlMapping;
import com.url.shortner.repository.DailyRepo;
import com.url.shortner.repository.GeoRepo;
import com.url.shortner.repository.HourlyRepo;
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
    private AnalyticsService analyticsService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private DailyRepo dailyRepo;

    @Autowired
    private HourlyRepo hourlyRepo;

    @Autowired
    private GeoRepo geoRepo;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
            .configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

    public String shortenUrl(String longUrl) {
        UrlMapping entity = new UrlMapping();
        entity.setLongUrl(longUrl);
        entity = urlRepository.save(entity);

        String shortCode = Base62Encoder.encode(entity.getId());
        entity.setShortCode(shortCode);
        urlRepository.save(entity);

        redisTemplate.opsForValue().set(
                "url:" + shortCode,
                longUrl,
                Duration.ofHours(24));

        redisTemplate.opsForValue().set("url:" + shortCode, longUrl);

        return shortCode;
    }

    public String getLongUrl(String shortCode) {

        String cachedUrl = (String) redisTemplate.opsForValue().get("url:" + shortCode);

        if (cachedUrl != null) {
            analyticsService.updateAnalytics(shortCode, "India"); // 🔥 FIX
            return cachedUrl;
        }

        UrlMapping mapping = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new RuntimeException("URL not found"));

        String longUrl = mapping.getLongUrl();

        redisTemplate.opsForValue().set(
                "url:" + shortCode,
                longUrl,
                Duration.ofHours(24));

        analyticsService.updateAnalytics(shortCode, "India"); // 🔥 FIX

        return longUrl;
    }

    public Map<String, Object> getAnalytics(String shortCode) {

        Map<String, Object> response = new HashMap<>();
        response.put("shortCode", shortCode);

        //1. Fetch DB data (historical)
        UrlMapping mapping = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new RuntimeException("URL not found"));

        int dbClicks = mapping.getClickCount();

        var dailyDb = dailyRepo.findByShortCode(shortCode);
        var hourlyDb = hourlyRepo.findByShortCode(shortCode);
        var geoDb = geoRepo.findByShortCode(shortCode);

        //2. Fetch Redis LIVE data
        Object redisClickObj = redisTemplate.opsForValue().get("click_total:" + shortCode);
        int redisClicks = redisClickObj != null ? Integer.parseInt(redisClickObj.toString()) : 0;

        Map<Object, Object> dailyRedis = redisTemplate.opsForHash().entries("daily:" + shortCode);
        Map<Object, Object> hourlyRedis = redisTemplate.opsForHash().entries("hourly:" + shortCode);
        Map<Object, Object> geoRedis = redisTemplate.opsForHash().entries("geo:" + shortCode);

        //3. Merge DAILY
        Map<String, Integer> finalDaily = new HashMap<>();

        for (DailyAnalytics d : dailyDb) {
            finalDaily.put(d.getDate().toString(), d.getCount());
        }

        for (Map.Entry<Object, Object> entry : dailyRedis.entrySet()) {
            String date = entry.getKey().toString();
            int count = Integer.parseInt(entry.getValue().toString());
            finalDaily.merge(date, count, Integer::sum);
        }

        //4. Merge HOURLY
        Map<String, Integer> finalHourly = new HashMap<>();

        for (HourlyAnalytics h : hourlyDb) {
            finalHourly.put(h.getHour().toString(), h.getCount());
        }

        for (Map.Entry<Object, Object> entry : hourlyRedis.entrySet()) {
            String hour = entry.getKey().toString();
            int count = Integer.parseInt(entry.getValue().toString());
            finalHourly.merge(hour, count, Integer::sum);
        }

        //5. Merge GEO
        Map<String, Integer> finalGeo = new HashMap<>();

        for (GeoAnalytics g : geoDb) {
            finalGeo.put(g.getCountry(), g.getCount());
        }

        for (Map.Entry<Object, Object> entry : geoRedis.entrySet()) {
            String country = entry.getKey().toString();
            int count = Integer.parseInt(entry.getValue().toString());
            finalGeo.merge(country, count, Integer::sum);
        }

        //6. Total clicks
        int totalClicks = dbClicks + redisClicks;

        // 7. Response
        response.put("totalClicks", totalClicks);
        response.put("dailyAnalytics", finalDaily);
        response.put("hourlyAnalytics", finalHourly);
        response.put("geoAnalytics", finalGeo);

        return response;
    }
}
