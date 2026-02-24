package com.url.shortner.repository;

import com.url.shortner.entity.UrlMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface UrlRepository extends JpaRepository<UrlMapping, Long> {
    UrlMapping findById(long id);

    Optional<UrlMapping> findByShortCode(String shortCode);

    @Transactional
    @Modifying
    @Query("UPDATE UrlMapping u SET u.clickCount = u.clickCount + :count WHERE u.shortCode = :shortCode")
    void incrementClickCount(@Param("shortCode") String shortCode, @Param("count") Integer count);
}
