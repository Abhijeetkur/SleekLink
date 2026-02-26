package com.url.shortner.service;

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
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.springframework.web.client.RestTemplate;
import java.util.Map;

@Service
public class KafkaConsumerService {

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

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @KafkaListener(topics = "click-events", groupId = "analytics-group")
    public void consume(String message) {

        String[] parts = message.split("\\|");

        String shortCode = parts[0];
        String ipAddress = parts[1];
        String country = resolveCountry(ipAddress);
        String os = parts[2];
        String browser = parts[3];
        String deviceType = parts[4];
        LocalDateTime time = LocalDateTime.parse(parts[5]);

        // TOTAL
        urlRepository.incrementClickCount(shortCode, 1);

        // DAILY
        LocalDate date = time.toLocalDate();

        DailyAnalytics d = dailyRepo.findByShortCodeAndDate(shortCode, date)
                .orElseGet(() -> {
                    DailyAnalytics newD = new DailyAnalytics();
                    newD.setShortCode(shortCode);
                    newD.setDate(date);
                    newD.setCount(0);
                    return newD;
                });

        d.setCount(d.getCount() + 1);
        dailyRepo.save(d);

        // HOURLY
        LocalDateTime hour = time.truncatedTo(ChronoUnit.HOURS);

        HourlyAnalytics h = hourlyRepo.findByShortCodeAndHour(shortCode, hour)
                .orElseGet(() -> {
                    HourlyAnalytics newH = new HourlyAnalytics();
                    newH.setShortCode(shortCode);
                    newH.setHour(hour);
                    newH.setCount(0);
                    return newH;
                });

        h.setCount(h.getCount() + 1);
        hourlyRepo.save(h);

        // GEO
        GeoAnalytics g = geoRepo.findByShortCodeAndCountry(shortCode, country)
                .orElseGet(() -> {
                    GeoAnalytics newG = new GeoAnalytics();
                    newG.setShortCode(shortCode);
                    newG.setCountry(country);
                    newG.setCount(0);
                    return newG;
                });

        g.setCount(g.getCount() + 1);
        geoRepo.save(g);

        // OS
        OsAnalytics o = osRepo.findByShortCodeAndOs(shortCode, os)
                .orElseGet(() -> {
                    OsAnalytics newO = new OsAnalytics();
                    newO.setShortCode(shortCode);
                    newO.setOs(os);
                    newO.setCount(0);
                    return newO;
                });

        o.setCount(o.getCount() + 1);
        osRepo.save(o);

        // BROWSER
        BrowserAnalytics b = browserRepo.findByShortCodeAndBrowser(shortCode, browser)
                .orElseGet(() -> {
                    BrowserAnalytics newB = new BrowserAnalytics();
                    newB.setShortCode(shortCode);
                    newB.setBrowser(browser);
                    newB.setCount(0);
                    return newB;
                });

        b.setCount(b.getCount() + 1);
        browserRepo.save(b);

        // DEVICE
        DeviceAnalytics dt = deviceRepo.findByShortCodeAndDevice(shortCode, deviceType)
                .orElseGet(() -> {
                    DeviceAnalytics newDt = new DeviceAnalytics();
                    newDt.setShortCode(shortCode);
                    newDt.setDevice(deviceType);
                    newDt.setCount(0);
                    return newDt;
                });

        dt.setCount(dt.getCount() + 1);
        deviceRepo.save(dt);

        // CQRS: Update Live View Read Cache
        // If the dashboard cache is hot, keep it perfectly in sync.
        // If it's cold, UrlService will rebuild it on the next read.
        if (Boolean.TRUE.equals(redisTemplate.hasKey("cache:total:" + shortCode))) {
            redisTemplate.opsForValue().increment("cache:total:" + shortCode);
            redisTemplate.opsForHash().increment("cache:daily:" + shortCode, date.toString(), 1);
            redisTemplate.opsForHash().increment("cache:hourly:" + shortCode, hour.toString(), 1);
            redisTemplate.opsForHash().increment("cache:geo:" + shortCode, country, 1);
            redisTemplate.opsForHash().increment("cache:os:" + shortCode, os, 1);
            redisTemplate.opsForHash().increment("cache:browser:" + shortCode, browser, 1);
            redisTemplate.opsForHash().increment("cache:device:" + shortCode, deviceType, 1);
            System.out.println("Updated hot Redis cache for dashboard");
        }
    }

    private String resolveCountry(String ipAddress) {
        if (ipAddress == null || ipAddress.isEmpty() || "127.0.0.1".equals(ipAddress)
                || "0:0:0:0:0:0:0:1".equals(ipAddress) || "unknown".equalsIgnoreCase(ipAddress)) {
            return "Unknown";
        }
        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = "http://ip-api.com/json/" + ipAddress;
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null && "success".equals(response.get("status"))) {
                return (String) response.get("country");
            }
        } catch (Exception e) {
            System.err.println("Error resolving IP: " + e.getMessage());
        }
        return "Unknown";
    }
}
