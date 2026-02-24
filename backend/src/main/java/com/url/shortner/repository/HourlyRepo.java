package com.url.shortner.repository;

import com.url.shortner.entity.HourlyAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface HourlyRepo extends JpaRepository<HourlyAnalytics, Long> {
    Optional<HourlyAnalytics> findByShortCodeAndHour(String shortCode, LocalDateTime hour);
}
