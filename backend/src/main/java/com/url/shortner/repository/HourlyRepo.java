package com.url.shortner.repository;

import com.url.shortner.entity.HourlyAnalytics;
import com.url.shortner.entity.UrlMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface HourlyRepo extends JpaRepository<HourlyAnalytics, Long> {
    Optional<HourlyAnalytics> findByUrlMappingAndHour(UrlMapping urlMapping, LocalDateTime hour);

    List<HourlyAnalytics> findByUrlMapping_ShortCode(String shortCode);
}
