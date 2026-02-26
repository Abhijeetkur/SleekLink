package com.url.shortner.repository;

import com.url.shortner.entity.DeviceAnalytics;
import com.url.shortner.entity.UrlMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceRepo extends JpaRepository<DeviceAnalytics, Long> {
    Optional<DeviceAnalytics> findByUrlMappingAndDevice(UrlMapping urlMapping, String device);

    List<DeviceAnalytics> findByUrlMapping_ShortCode(String shortCode);
}
