package com.url.shortner.repository;

import com.url.shortner.entity.BrowserAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BrowserRepo extends JpaRepository<BrowserAnalytics, Long> {
    List<BrowserAnalytics> findByShortCode(String shortCode);

    Optional<BrowserAnalytics> findByShortCodeAndBrowser(String shortCode, String browser);
}
