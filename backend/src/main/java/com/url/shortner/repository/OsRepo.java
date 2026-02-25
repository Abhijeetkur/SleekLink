package com.url.shortner.repository;

import com.url.shortner.entity.OsAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OsRepo extends JpaRepository<OsAnalytics, Long> {
    List<OsAnalytics> findByShortCode(String shortCode);

    Optional<OsAnalytics> findByShortCodeAndOs(String shortCode, String os);
}
