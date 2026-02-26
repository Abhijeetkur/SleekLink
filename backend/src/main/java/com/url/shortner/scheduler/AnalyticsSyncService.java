package com.url.shortner.scheduler;

import com.url.shortner.entity.DailyAnalytics;
import com.url.shortner.entity.GeoAnalytics;
import com.url.shortner.entity.HourlyAnalytics;
import com.url.shortner.entity.OsAnalytics;
import com.url.shortner.entity.DeviceAnalytics;
import com.url.shortner.entity.BrowserAnalytics;
import com.url.shortner.repository.DailyRepo;
import com.url.shortner.repository.GeoRepo;
import com.url.shortner.repository.HourlyRepo;
import com.url.shortner.repository.OsRepo;
import com.url.shortner.repository.DeviceRepo;
import com.url.shortner.repository.BrowserRepo;
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
    @Autowired
    private OsRepo osRepo;
    @Autowired
    private DeviceRepo deviceRepo;
    @Autowired
    private BrowserRepo browserRepo;

    // Disabled cron - Kafka now processes events in real-time
    // @Scheduled(fixedDelay = 60000)
    public void syncAllAnalytics() {

        // 1. TOTAL CLICKS (SAFE VERSION)
        Set<String> totalKeys = redisTemplate.keys("click_total:*");

        if (totalKeys != null) {
            for (String key : totalKeys) {

                String shortCode = key.replace("click_total:", "");
                String tempKey = key + ":processing";

                // Atomic rename to avoid race condition
                Boolean renamed = redisTemplate.renameIfAbsent(key, tempKey);
                if (Boolean.FALSE.equals(renamed))
                    continue;

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
                if (Boolean.FALSE.equals(renamed))
                    continue;

                try {
                    com.url.shortner.entity.UrlMapping mapping = urlRepository.findByShortCode(shortCode).orElse(null);
                    if (mapping == null) {
                        redisTemplate.delete(tempKey);
                        continue;
                    }
                    Map<Object, Object> map = redisTemplate.opsForHash().entries(tempKey);

                    for (Map.Entry<Object, Object> entry : map.entrySet()) {

                        LocalDate date = LocalDate.parse(entry.getKey().toString());
                        int count = Integer.parseInt(entry.getValue().toString());

                        DailyAnalytics d = dailyRepo.findByUrlMappingAndDate(mapping, date)
                                .orElseGet(() -> {
                                    DailyAnalytics newD = new DailyAnalytics();
                                    newD.setUrlMapping(mapping);
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
                if (Boolean.FALSE.equals(renamed))
                    continue;

                try {
                    com.url.shortner.entity.UrlMapping mapping = urlRepository.findByShortCode(shortCode).orElse(null);
                    if (mapping == null) {
                        redisTemplate.delete(tempKey);
                        continue;
                    }
                    Map<Object, Object> map = redisTemplate.opsForHash().entries(tempKey);

                    for (Map.Entry<Object, Object> entry : map.entrySet()) {

                        LocalDateTime hour = LocalDateTime.parse(entry.getKey().toString());
                        int count = Integer.parseInt(entry.getValue().toString());

                        HourlyAnalytics h = hourlyRepo.findByUrlMappingAndHour(mapping, hour)
                                .orElseGet(() -> {
                                    HourlyAnalytics newH = new HourlyAnalytics();
                                    newH.setUrlMapping(mapping);
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
                if (Boolean.FALSE.equals(renamed))
                    continue;

                try {
                    com.url.shortner.entity.UrlMapping mapping = urlRepository.findByShortCode(shortCode).orElse(null);
                    if (mapping == null) {
                        redisTemplate.delete(tempKey);
                        continue;
                    }
                    Map<Object, Object> map = redisTemplate.opsForHash().entries(tempKey);

                    for (Map.Entry<Object, Object> entry : map.entrySet()) {

                        String country = entry.getKey().toString();
                        int count = Integer.parseInt(entry.getValue().toString());

                        GeoAnalytics g = geoRepo.findByUrlMappingAndCountry(mapping, country)
                                .orElseGet(() -> {
                                    GeoAnalytics newG = new GeoAnalytics();
                                    newG.setUrlMapping(mapping);
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

        // 4.1 OS SYNC
        Set<String> osKeys = redisTemplate.keys("os:*");
        if (osKeys != null) {
            for (String key : osKeys) {
                String shortCode = key.replace("os:", "");
                String tempKey = key + ":processing";

                Boolean renamed = redisTemplate.renameIfAbsent(key, tempKey);
                if (Boolean.FALSE.equals(renamed))
                    continue;

                try {
                    com.url.shortner.entity.UrlMapping mapping = urlRepository.findByShortCode(shortCode).orElse(null);
                    if (mapping == null) {
                        redisTemplate.delete(tempKey);
                        continue;
                    }
                    Map<Object, Object> map = redisTemplate.opsForHash().entries(tempKey);
                    for (Map.Entry<Object, Object> entry : map.entrySet()) {
                        String os = entry.getKey().toString();
                        int count = Integer.parseInt(entry.getValue().toString());

                        OsAnalytics o = osRepo.findByUrlMappingAndOs(mapping, os)
                                .orElseGet(() -> {
                                    OsAnalytics newO = new OsAnalytics();
                                    newO.setUrlMapping(mapping);
                                    newO.setOs(os);
                                    newO.setCount(0);
                                    return newO;
                                });

                        o.setCount(o.getCount() + count);
                        osRepo.save(o);
                    }
                    redisTemplate.delete(tempKey);
                } catch (Exception e) {
                    System.out.println("Error syncing os: " + e.getMessage());
                }
            }
        }

        // 4.2 DEVICE SYNC
        Set<String> deviceKeys = redisTemplate.keys("device:*");
        if (deviceKeys != null) {
            for (String key : deviceKeys) {
                String shortCode = key.replace("device:", "");
                String tempKey = key + ":processing";

                Boolean renamed = redisTemplate.renameIfAbsent(key, tempKey);
                if (Boolean.FALSE.equals(renamed))
                    continue;

                try {
                    com.url.shortner.entity.UrlMapping mapping = urlRepository.findByShortCode(shortCode).orElse(null);
                    if (mapping == null) {
                        redisTemplate.delete(tempKey);
                        continue;
                    }
                    Map<Object, Object> map = redisTemplate.opsForHash().entries(tempKey);
                    for (Map.Entry<Object, Object> entry : map.entrySet()) {
                        String device = entry.getKey().toString();
                        int count = Integer.parseInt(entry.getValue().toString());

                        DeviceAnalytics d = deviceRepo.findByUrlMappingAndDevice(mapping, device)
                                .orElseGet(() -> {
                                    DeviceAnalytics newD = new DeviceAnalytics();
                                    newD.setUrlMapping(mapping);
                                    newD.setDevice(device);
                                    newD.setCount(0);
                                    return newD;
                                });

                        d.setCount(d.getCount() + count);
                        deviceRepo.save(d);
                    }
                    redisTemplate.delete(tempKey);
                } catch (Exception e) {
                    System.out.println("Error syncing device: " + e.getMessage());
                }
            }
        }

        // 4.3 BROWSER SYNC
        Set<String> browserKeys = redisTemplate.keys("browser:*");
        if (browserKeys != null) {
            for (String key : browserKeys) {
                String shortCode = key.replace("browser:", "");
                String tempKey = key + ":processing";

                Boolean renamed = redisTemplate.renameIfAbsent(key, tempKey);
                if (Boolean.FALSE.equals(renamed))
                    continue;

                try {
                    com.url.shortner.entity.UrlMapping mapping = urlRepository.findByShortCode(shortCode).orElse(null);
                    if (mapping == null) {
                        redisTemplate.delete(tempKey);
                        continue;
                    }
                    Map<Object, Object> map = redisTemplate.opsForHash().entries(tempKey);
                    for (Map.Entry<Object, Object> entry : map.entrySet()) {
                        String browser = entry.getKey().toString();
                        int count = Integer.parseInt(entry.getValue().toString());

                        BrowserAnalytics b = browserRepo.findByUrlMappingAndBrowser(mapping, browser)
                                .orElseGet(() -> {
                                    BrowserAnalytics newB = new BrowserAnalytics();
                                    newB.setUrlMapping(mapping);
                                    newB.setBrowser(browser);
                                    newB.setCount(0);
                                    return newB;
                                });

                        b.setCount(b.getCount() + count);
                        browserRepo.save(b);
                    }
                    redisTemplate.delete(tempKey);
                } catch (Exception e) {
                    System.out.println("Error syncing browser: " + e.getMessage());
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
