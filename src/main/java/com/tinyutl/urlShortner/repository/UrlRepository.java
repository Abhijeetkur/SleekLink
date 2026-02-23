package com.tinyutl.urlShortner.repository;

import com.tinyutl.urlShortner.entity.UrlMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UrlRepository extends JpaRepository<UrlMapping, Long> {
    UrlMapping findById(long id);
    Optional<UrlMapping> findByShortCode(String shortCode);
}
