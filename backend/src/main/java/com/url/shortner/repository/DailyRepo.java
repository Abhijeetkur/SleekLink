package com.url.shortner.repository;

import com.url.shortner.entity.DailyAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface DailyRepo extends JpaRepository<DailyAnalytics, Long> {
    Optional<DailyAnalytics> findByShortCodeAndDate(String shortCode, LocalDate date);
}
