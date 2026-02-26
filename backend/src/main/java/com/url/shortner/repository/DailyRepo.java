package com.url.shortner.repository;

import com.url.shortner.entity.DailyAnalytics;
import com.url.shortner.entity.UrlMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyRepo extends JpaRepository<DailyAnalytics, Long> {
    Optional<DailyAnalytics> findByUrlMappingAndDate(UrlMapping urlMapping, LocalDate date);

    List<DailyAnalytics> findByUrlMapping_ShortCode(String shortCode);
}
