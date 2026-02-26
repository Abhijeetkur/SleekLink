package com.url.shortner.repository;

import com.url.shortner.entity.OsAnalytics;
import com.url.shortner.entity.UrlMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OsRepo extends JpaRepository<OsAnalytics, Long> {
    Optional<OsAnalytics> findByUrlMappingAndOs(UrlMapping urlMapping, String os);

    List<OsAnalytics> findByUrlMapping_ShortCode(String shortCode);
}
