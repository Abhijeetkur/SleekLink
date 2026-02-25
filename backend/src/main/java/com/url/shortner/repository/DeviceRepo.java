package com.url.shortner.repository;

import com.url.shortner.entity.DeviceAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceRepo extends JpaRepository<DeviceAnalytics, Long> {
    List<DeviceAnalytics> findByShortCode(String shortCode);

    Optional<DeviceAnalytics> findByShortCodeAndDevice(String shortCode, String device);
}
