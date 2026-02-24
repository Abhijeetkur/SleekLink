package com.url.shortner.repository;

import com.url.shortner.entity.GeoAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GeoRepo extends JpaRepository<GeoAnalytics, Long> {
    Optional<GeoAnalytics> findByShortCodeAndCountry(String shortCode, String country);
}
