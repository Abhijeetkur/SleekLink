package com.url.shortner.repository;

import com.url.shortner.entity.GeoAnalytics;
import com.url.shortner.entity.UrlMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GeoRepo extends JpaRepository<GeoAnalytics, Long> {
    Optional<GeoAnalytics> findByUrlMappingAndCountry(UrlMapping urlMapping, String country);

    List<GeoAnalytics> findByUrlMapping_ShortCode(String shortCode);
}
