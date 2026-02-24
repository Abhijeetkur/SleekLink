package com.url.shortner.service;

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

        // 🔥 1. TOTAL CLICKS
        Set<String> totalKeys = redisTemplate.keys("click_total:*");

        if (totalKeys != null) {
            for (String key : totalKeys) {
                String shortCode = key.replace("click_total:", "");

                Object value = redisTemplate.opsForValue().get(key);
                if (value != null) {
                    int total = Integer.parseInt(value.toString());
                    urlRepository.incrementClickCount(shortCode, total);
                }
            }
        }

        // 🔥 2. DAILY
        Set<String> dailyKeys = redisTemplate.keys("daily:*");

        if (dailyKeys != null) {
            for (String key : dailyKeys) {
                String shortCode = key.replace("daily:", "");

                Map<Object, Object> map = redisTemplate.opsForHash().entries(key);

                for (Map.Entry<Object, Object> entry : map.entrySet()) {
                    LocalDate date = LocalDate.parse(entry.getKey().toString());
                    int count = Integer.parseInt(entry.getValue().toString());

                    DailyAnalytics d = dailyRepo.findByShortCodeAndDate(shortCode, date)
                            .orElseGet(() -> {
                                DailyAnalytics newD = new DailyAnalytics();
                                newD.setShortCode(shortCode);
                                newD.setDate(date);
                                return newD;
                            });

                    d.setCount(count);
                    dailyRepo.save(d);
                }
            }
        }

        // 🔥 3. HOURLY
        Set<String> hourlyKeys = redisTemplate.keys("hourly:*");

        if (hourlyKeys != null) {
            for (String key : hourlyKeys) {
                String shortCode = key.replace("hourly:", "");

                Map<Object, Object> map = redisTemplate.opsForHash().entries(key);

                for (Map.Entry<Object, Object> entry : map.entrySet()) {
                    LocalDateTime hour = LocalDateTime.parse(entry.getKey().toString());
                    int count = Integer.parseInt(entry.getValue().toString());

                    HourlyAnalytics h = hourlyRepo.findByShortCodeAndHour(shortCode, hour)
                            .orElseGet(() -> {
                                HourlyAnalytics newH = new HourlyAnalytics();
                                newH.setShortCode(shortCode);
                                newH.setHour(hour);
                                return newH;
                            });

                    h.setCount(count);
                    hourlyRepo.save(h);
                }
            }
        }

        // 🔥 4. GEO
        Set<String> geoKeys = redisTemplate.keys("geo:*");

        if (geoKeys != null) {
            for (String key : geoKeys) {
                String shortCode = key.replace("geo:", "");

                Map<Object, Object> map = redisTemplate.opsForHash().entries(key);

                for (Map.Entry<Object, Object> entry : map.entrySet()) {
                    String country = entry.getKey().toString();
                    int count = Integer.parseInt(entry.getValue().toString());

                    GeoAnalytics g = geoRepo.findByShortCodeAndCountry(shortCode, country)
                            .orElseGet(() -> {
                                GeoAnalytics newG = new GeoAnalytics();
                                newG.setShortCode(shortCode);
                                newG.setCountry(country);
                                return newG;
                            });

                    g.setCount(count);
                    geoRepo.save(g);
                }
            }
        }

        // 🔥 5. TRENDING (optional DB store)
        Set<Object> trending = redisTemplate.opsForZSet()
                .reverseRange("trending_urls", 0, 10);

        System.out.println("Top trending: " + trending);
    }
}
