package com.url.shortner.service;

import com.url.shortner.repository.UrlRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.url.shortner.encoder.Base62Encoder;
import com.url.shortner.entity.UrlMapping;
import com.url.shortner.repository.DailyRepo;
import com.url.shortner.repository.GeoRepo;
import com.url.shortner.repository.HourlyRepo;
import com.url.shortner.repository.OsRepo;
import com.url.shortner.repository.DeviceRepo;
import com.url.shortner.repository.BrowserRepo;
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
    private AnalyticsService analyticsService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private DailyRepo dailyRepo;

    @Autowired
    private HourlyRepo hourlyRepo;

    @Autowired
    private GeoRepo geoRepo;

    @Autowired
    private OsRepo osRepo;

    @Autowired
    private DeviceRepo deviceRepo;

    @Autowired
    private BrowserRepo browserRepo;

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

    public String getLongUrl(String shortCode, String userAgent, String ipAddress) {

        String cachedUrl = (String) redisTemplate.opsForValue().get("url:" + shortCode);

        if (cachedUrl != null) {
            analyticsService.updateAnalytics(shortCode, ipAddress, userAgent); // FIX
            return cachedUrl;
        }

        UrlMapping mapping = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new RuntimeException("URL not found"));

        String longUrl = mapping.getLongUrl();

        redisTemplate.opsForValue().set(
                "url:" + shortCode,
                longUrl,
                Duration.ofHours(24));

        analyticsService.updateAnalytics(shortCode, ipAddress, userAgent); // FIX

        return longUrl;
    }

    public Map<String, Object> getAnalytics(String shortCode) {

        Map<String, Object> response = new HashMap<>();
        response.put("shortCode", shortCode);

        String totalKey = "cache:total:" + shortCode;

        //  1. If Cache doesn't exist, Load entirely from DB & populate Cache
        if (Boolean.FALSE.equals(redisTemplate.hasKey(totalKey))) {
            System.out.println("Cache missed, loading analytics from DB to Redis...");
            UrlMapping mapping = urlRepository.findByShortCode(shortCode)
                    .orElseThrow(() -> new RuntimeException("URL not found"));

            int dbClicks = mapping.getClickCount();

            // Push Total
            redisTemplate.opsForValue().set(totalKey, String.valueOf(dbClicks), Duration.ofHours(24));

            // Push Daily
            Map<String, String> dailyMap = new HashMap<>();
            dailyRepo.findByShortCode(shortCode).forEach(d -> dailyMap.put(d.getDate().toString(), String.valueOf(d.getCount())));
            if (!dailyMap.isEmpty()) redisTemplate.opsForHash().putAll("cache:daily:" + shortCode, dailyMap);
            redisTemplate.expire("cache:daily:" + shortCode, Duration.ofHours(24));

            // Push Hourly
            Map<String, String> hourlyMap = new HashMap<>();
            hourlyRepo.findByShortCode(shortCode).forEach(h -> hourlyMap.put(h.getHour().toString(), String.valueOf(h.getCount())));
            if (!hourlyMap.isEmpty()) redisTemplate.opsForHash().putAll("cache:hourly:" + shortCode, hourlyMap);
            redisTemplate.expire("cache:hourly:" + shortCode, Duration.ofHours(24));

            // Push Geo
            Map<String, String> geoMap = new HashMap<>();
            geoRepo.findByShortCode(shortCode).forEach(g -> geoMap.put(g.getCountry(), String.valueOf(g.getCount())));
            if (!geoMap.isEmpty()) redisTemplate.opsForHash().putAll("cache:geo:" + shortCode, geoMap);
            redisTemplate.expire("cache:geo:" + shortCode, Duration.ofHours(24));

            // Push OS
            Map<String, String> osMap = new HashMap<>();
            osRepo.findByShortCode(shortCode).forEach(o -> osMap.put(o.getOs(), String.valueOf(o.getCount())));
            if (!osMap.isEmpty()) redisTemplate.opsForHash().putAll("cache:os:" + shortCode, osMap);
            redisTemplate.expire("cache:os:" + shortCode, Duration.ofHours(24));

            // Push Device
            Map<String, String> deviceMap = new HashMap<>();
            deviceRepo.findByShortCode(shortCode).forEach(d -> deviceMap.put(d.getDevice(), String.valueOf(d.getCount())));
            if (!deviceMap.isEmpty()) redisTemplate.opsForHash().putAll("cache:device:" + shortCode, deviceMap);
            redisTemplate.expire("cache:device:" + shortCode, Duration.ofHours(24));

            // Push Browser
            Map<String, String> browserMap = new HashMap<>();
            browserRepo.findByShortCode(shortCode).forEach(b -> browserMap.put(b.getBrowser(), String.valueOf(b.getCount())));
            if (!browserMap.isEmpty()) redisTemplate.opsForHash().putAll("cache:browser:" + shortCode, browserMap);
            redisTemplate.expire("cache:browser:" + shortCode, Duration.ofHours(24));
        }

        //  2. Read Everything Directly from Redis Cache! (Zero Math, Real-Time)
        Object totalClicksObj = redisTemplate.opsForValue().get(totalKey);
        int totalClicks = totalClicksObj != null ? Integer.parseInt(totalClicksObj.toString()) : 0;

        Map<Object, Object> dailyAnalytics = redisTemplate.opsForHash().entries("cache:daily:" + shortCode);
        Map<Object, Object> hourlyAnalytics = redisTemplate.opsForHash().entries("cache:hourly:" + shortCode);
        Map<Object, Object> geoAnalytics = redisTemplate.opsForHash().entries("cache:geo:" + shortCode);
        Map<Object, Object> osAnalytics = redisTemplate.opsForHash().entries("cache:os:" + shortCode);
        Map<Object, Object> deviceAnalytics = redisTemplate.opsForHash().entries("cache:device:" + shortCode);
        Map<Object, Object> browserAnalytics = redisTemplate.opsForHash().entries("cache:browser:" + shortCode);

        // Map directly to response
        response.put("totalClicks", totalClicks);
        response.put("dailyAnalytics", parseMap(dailyAnalytics));
        response.put("hourlyAnalytics", parseMap(hourlyAnalytics));
        response.put("geoAnalytics", parseMap(geoAnalytics));
        response.put("osAnalytics", parseMap(osAnalytics));
        response.put("deviceAnalytics", parseMap(deviceAnalytics));
        response.put("browserAnalytics", parseMap(browserAnalytics));

        return response;
    }

    private Map<String, Integer> parseMap(Map<Object, Object> source) {
        Map<String, Integer> result = new HashMap<>();
        for (Map.Entry<Object, Object> entry : source.entrySet()) {
            result.put(entry.getKey().toString(), Integer.parseInt(entry.getValue().toString()));
        }
        return result;
    }
}
