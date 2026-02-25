package com.url.shortner.service;

import com.fasterxml.jackson.core.type.TypeReference;
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
import com.url.shortner.repository.OsRepo;
import com.url.shortner.repository.DeviceRepo;
import com.url.shortner.repository.BrowserRepo;
import com.url.shortner.entity.OsAnalytics;
import com.url.shortner.entity.DeviceAnalytics;
import com.url.shortner.entity.BrowserAnalytics;
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
            analyticsService.updateAnalytics(shortCode, ipAddress, userAgent); // 🔥 FIX
            return cachedUrl;
        }

        UrlMapping mapping = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new RuntimeException("URL not found"));

        String longUrl = mapping.getLongUrl();

        redisTemplate.opsForValue().set(
                "url:" + shortCode,
                longUrl,
                Duration.ofHours(24));

        analyticsService.updateAnalytics(shortCode, ipAddress, userAgent); // 🔥 FIX

        return longUrl;
    }

    public Map<String, Object> getAnalytics(String shortCode) {

        Map<String, Object> response = new HashMap<>();
        response.put("shortCode", shortCode);

        String snapshotKey = "analytics_db_snapshot:" + shortCode;

        Map<String, Object> dbSnapshot = null;

        try {
            // 🔥 1. Try to get DB snapshot from Redis
            String cachedJson = (String) redisTemplate.opsForValue().get(snapshotKey);

            if (cachedJson != null) {
                System.out.println("snapshot found");
                System.out.println(objectMapper.readTree(cachedJson));
                dbSnapshot = objectMapper.readValue(
                        cachedJson,
                        new TypeReference<Map<String, Object>>() {
                        });
            }
        } catch (Exception e) {
            dbSnapshot = null; // fallback to DB
        }

        // 🔥 2. If snapshot not found → fetch from DB
        if (dbSnapshot == null) {
            System.out.println("snapshot not found so db hit");

            UrlMapping mapping = urlRepository.findByShortCode(shortCode)
                    .orElseThrow(() -> new RuntimeException("URL not found"));

            int dbClicks = mapping.getClickCount();

            var dailyDb = dailyRepo.findByShortCode(shortCode);
            var hourlyDb = hourlyRepo.findByShortCode(shortCode);
            var geoDb = geoRepo.findByShortCode(shortCode);
            var osDb = osRepo.findByShortCode(shortCode);
            var deviceDb = deviceRepo.findByShortCode(shortCode);
            var browserDb = browserRepo.findByShortCode(shortCode);

            // Convert DB objects → Map
            Map<String, Integer> dailyMap = new HashMap<>();
            for (DailyAnalytics d : dailyDb) {
                dailyMap.put(d.getDate().toString(), d.getCount());
            }

            Map<String, Integer> hourlyMap = new HashMap<>();
            for (HourlyAnalytics h : hourlyDb) {
                hourlyMap.put(h.getHour().toString(), h.getCount());
            }

            Map<String, Integer> geoMap = new HashMap<>();
            for (GeoAnalytics g : geoDb) {
                geoMap.put(g.getCountry(), g.getCount());
            }

            Map<String, Integer> osMap = new HashMap<>();
            for (OsAnalytics o : osDb) {
                osMap.put(o.getOs(), o.getCount());
            }

            Map<String, Integer> deviceMap = new HashMap<>();
            for (DeviceAnalytics d : deviceDb) {
                deviceMap.put(d.getDevice(), d.getCount());
            }

            Map<String, Integer> browserMap = new HashMap<>();
            for (BrowserAnalytics b : browserDb) {
                browserMap.put(b.getBrowser(), b.getCount());
            }

            dbSnapshot = new HashMap<>();
            dbSnapshot.put("dbClicks", dbClicks);
            dbSnapshot.put("dailyAnalytics", dailyMap);
            dbSnapshot.put("hourlyAnalytics", hourlyMap);
            dbSnapshot.put("geoAnalytics", geoMap);
            dbSnapshot.put("osAnalytics", osMap);
            dbSnapshot.put("deviceAnalytics", deviceMap);
            dbSnapshot.put("browserAnalytics", browserMap);

            // 🔥 3. Cache snapshot in Redis (short TTL)
            try {
                redisTemplate.opsForValue().set(
                        snapshotKey,
                        objectMapper.writeValueAsString(dbSnapshot),
                        Duration.ofSeconds(20) // 🔥 tune this
                );
            } catch (Exception e) {
                System.out.println("Snapshot cache failed: " + e.getMessage());
            }
        }

        // 🔥 4. Extract snapshot data
        int dbClicks = (Integer) dbSnapshot.get("dbClicks");

        Map<String, Integer> finalDaily = (Map<String, Integer>) dbSnapshot.get("dailyAnalytics");

        Map<String, Integer> finalHourly = (Map<String, Integer>) dbSnapshot.get("hourlyAnalytics");

        Map<String, Integer> finalGeo = (Map<String, Integer>) dbSnapshot.get("geoAnalytics");

        Map<String, Integer> finalOs = (Map<String, Integer>) dbSnapshot.get("osAnalytics");

        Map<String, Integer> finalDevice = (Map<String, Integer>) dbSnapshot.get("deviceAnalytics");

        Map<String, Integer> finalBrowser = (Map<String, Integer>) dbSnapshot.get("browserAnalytics");

        // 🔥 5. Fetch LIVE Redis data
        Object redisClickObj = redisTemplate.opsForValue().get("click_total:" + shortCode);
        int redisClicks = redisClickObj != null ? Integer.parseInt(redisClickObj.toString()) : 0;

        Map<Object, Object> dailyRedis = redisTemplate.opsForHash().entries("daily:" + shortCode);
        Map<Object, Object> hourlyRedis = redisTemplate.opsForHash().entries("hourly:" + shortCode);
        Map<Object, Object> geoRedis = redisTemplate.opsForHash().entries("geo:" + shortCode);

        // 🔥 5.5 Fetch OS, Device, and Browser data
        Map<Object, Object> osRedis = redisTemplate.opsForHash().entries("os:" + shortCode);
        Map<Object, Object> deviceRedis = redisTemplate.opsForHash().entries("device:" + shortCode);
        Map<Object, Object> browserRedis = redisTemplate.opsForHash().entries("browser:" + shortCode);

        // Convert OS, Device, Browser to Maps for the response
        for (Map.Entry<Object, Object> entry : osRedis.entrySet()) {
            finalOs.merge(entry.getKey().toString(), Integer.parseInt(entry.getValue().toString()), Integer::sum);
        }

        for (Map.Entry<Object, Object> entry : deviceRedis.entrySet()) {
            finalDevice.merge(entry.getKey().toString(), Integer.parseInt(entry.getValue().toString()), Integer::sum);
        }

        for (Map.Entry<Object, Object> entry : browserRedis.entrySet()) {
            finalBrowser.merge(entry.getKey().toString(), Integer.parseInt(entry.getValue().toString()), Integer::sum);
        }

        // 🔥 6. Merge DAILY
        for (Map.Entry<Object, Object> entry : dailyRedis.entrySet()) {
            String date = entry.getKey().toString();
            int count = Integer.parseInt(entry.getValue().toString());
            finalDaily.merge(date, count, Integer::sum);
        }

        // 🔥 7. Merge HOURLY
        for (Map.Entry<Object, Object> entry : hourlyRedis.entrySet()) {
            String hour = entry.getKey().toString();
            int count = Integer.parseInt(entry.getValue().toString());
            finalHourly.merge(hour, count, Integer::sum);
        }

        // 🔥 8. Merge GEO
        for (Map.Entry<Object, Object> entry : geoRedis.entrySet()) {
            String country = entry.getKey().toString();
            int count = Integer.parseInt(entry.getValue().toString());
            finalGeo.merge(country, count, Integer::sum);
        }

        // 🔥 9. Total clicks
        int totalClicks = dbClicks + redisClicks;

        // 🔥 10. Response
        response.put("totalClicks", totalClicks);
        response.put("dailyAnalytics", finalDaily);
        response.put("hourlyAnalytics", finalHourly);
        response.put("geoAnalytics", finalGeo);
        response.put("osAnalytics", finalOs);
        response.put("deviceAnalytics", finalDevice);
        response.put("browserAnalytics", finalBrowser);

        return response;
    }
}
