package com.url.shortner.repository;

import com.url.shortner.entity.BrowserAnalytics;
import com.url.shortner.entity.UrlMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BrowserRepo extends JpaRepository<BrowserAnalytics, Long> {
    List<BrowserAnalytics> findByUrlMapping_ShortCode(String shortCode);

    Optional<BrowserAnalytics> findByUrlMappingAndBrowser(UrlMapping urlMapping, String browser);
}
