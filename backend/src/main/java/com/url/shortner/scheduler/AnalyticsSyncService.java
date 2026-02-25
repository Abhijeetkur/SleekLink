package com.url.shortner.scheduler;

import com.url.shortner.entity.DailyAnalytics;
import com.url.shortner.entity.GeoAnalytics;
import com.url.shortner.entity.HourlyAnalytics;
import com.url.shortner.repository.DailyRepo;
import com.url.shortner.repository.GeoRepo;
import com.url.shortner.repository.HourlyRepo;
import com.url.shortner.repository.UrlRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

@Service
public class AnalyticsSyncService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private UrlRepository urlRepository;
    @Autowired
    private DailyRepo dailyRepo;
    @Autowired
    private HourlyRepo hourlyRepo;
    @Autowired
    private GeoRepo geoRepo;

    @Scheduled(fixedRate = 60000)
    public void syncAllAnalytics() {

        // 1. TOTAL CLICKS (SAFE VERSION)
        Set<String> totalKeys = redisTemplate.keys("click_total:*");

        if (totalKeys != null) {
            for (String key : totalKeys) {

                String shortCode = key.replace("click_total:", "");
                String tempKey = key + ":processing";

                // Atomic rename to avoid race condition
                Boolean renamed = redisTemplate.renameIfAbsent(key, tempKey);
                if (Boolean.FALSE.equals(renamed)) continue;

                try {
                    Object value = redisTemplate.opsForValue().get(tempKey);

                    if (value != null) {
                        int total = Integer.parseInt(value.toString());
                        urlRepository.incrementClickCount(shortCode, total);
                    }

                    // delete only after success
                    redisTemplate.delete(tempKey);

                } catch (Exception e) {
                    System.out.println("Error syncing total clicks: " + e.getMessage());
                }
            }
        }

        // 2. DAILY (SAFE VERSION)
        Set<String> dailyKeys = redisTemplate.keys("daily:*");

        if (dailyKeys != null) {
            for (String key : dailyKeys) {

                String shortCode = key.replace("daily:", "");
                String tempKey = key + ":processing";

                Boolean renamed = redisTemplate.renameIfAbsent(key, tempKey);
                if (Boolean.FALSE.equals(renamed)) continue;

                try {
                    Map<Object, Object> map = redisTemplate.opsForHash().entries(tempKey);

                    for (Map.Entry<Object, Object> entry : map.entrySet()) {

                        LocalDate date = LocalDate.parse(entry.getKey().toString());
                        int count = Integer.parseInt(entry.getValue().toString());

                        DailyAnalytics d = dailyRepo.findByShortCodeAndDate(shortCode, date)
                                .orElseGet(() -> {
                                    DailyAnalytics newD = new DailyAnalytics();
                                    newD.setShortCode(shortCode);
                                    newD.setDate(date);
                                    newD.setCount(0);
                                    return newD;
                                });

                        d.setCount(d.getCount() + count);
                        dailyRepo.save(d);
                    }

                    redisTemplate.delete(tempKey);

                } catch (Exception e) {
                    System.out.println("Error syncing daily: " + e.getMessage());
                }
            }
        }

        // 3. HOURLY (SAFE VERSION)
        Set<String> hourlyKeys = redisTemplate.keys("hourly:*");

        if (hourlyKeys != null) {
            for (String key : hourlyKeys) {

                String shortCode = key.replace("hourly:", "");
                String tempKey = key + ":processing";

                Boolean renamed = redisTemplate.renameIfAbsent(key, tempKey);
                if (Boolean.FALSE.equals(renamed)) continue;

                try {
                    Map<Object, Object> map = redisTemplate.opsForHash().entries(tempKey);

                    for (Map.Entry<Object, Object> entry : map.entrySet()) {

                        LocalDateTime hour = LocalDateTime.parse(entry.getKey().toString());
                        int count = Integer.parseInt(entry.getValue().toString());

                        HourlyAnalytics h = hourlyRepo.findByShortCodeAndHour(shortCode, hour)
                                .orElseGet(() -> {
                                    HourlyAnalytics newH = new HourlyAnalytics();
                                    newH.setShortCode(shortCode);
                                    newH.setHour(hour);
                                    newH.setCount(0);
                                    return newH;
                                });

                        h.setCount(h.getCount() + count);
                        hourlyRepo.save(h);
                    }

                    redisTemplate.delete(tempKey);

                } catch (Exception e) {
                    System.out.println("Error syncing hourly: " + e.getMessage());
                }
            }
        }

        // 4. GEO (SAFE VERSION)
        Set<String> geoKeys = redisTemplate.keys("geo:*");

        if (geoKeys != null) {
            for (String key : geoKeys) {

                String shortCode = key.replace("geo:", "");
                String tempKey = key + ":processing";

                Boolean renamed = redisTemplate.renameIfAbsent(key, tempKey);
                if (Boolean.FALSE.equals(renamed)) continue;

                try {
                    Map<Object, Object> map = redisTemplate.opsForHash().entries(tempKey);

                    for (Map.Entry<Object, Object> entry : map.entrySet()) {

                        String country = entry.getKey().toString();
                        int count = Integer.parseInt(entry.getValue().toString());

                        GeoAnalytics g = geoRepo.findByShortCodeAndCountry(shortCode, country)
                                .orElseGet(() -> {
                                    GeoAnalytics newG = new GeoAnalytics();
                                    newG.setShortCode(shortCode);
                                    newG.setCountry(country);
                                    newG.setCount(0);
                                    return newG;
                                });

                        g.setCount(g.getCount() + count);
                        geoRepo.save(g);
                    }

                    redisTemplate.delete(tempKey);

                } catch (Exception e) {
                    System.out.println("Error syncing geo: " + e.getMessage());
                }
            }
        }

        // 5. TRENDING
        Set<Object> trending = redisTemplate.opsForZSet()
                .reverseRange("trending_urls", 0, 10);

        System.out.println("Top trending: " + trending);

        // 6. SNAPSHOT INVALIDATION (VERY IMPORTANT)
        Set<String> snapshotKeys = redisTemplate.keys("analytics_db_snapshot:*");
        if (snapshotKeys != null && !snapshotKeys.isEmpty()) {
            redisTemplate.delete(snapshotKeys);
        }
    }
}
