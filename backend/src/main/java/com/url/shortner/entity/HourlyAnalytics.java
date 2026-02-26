package com.url.shortner.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "hourly_analytics", indexes = {
                @Index(name = "idx_hourly_url_mapping_hour", columnList = "url_mapping_id, hour")
}, uniqueConstraints = {
                @UniqueConstraint(columnNames = { "url_mapping_id", "hour" })
})
@Getter
@Setter
public class HourlyAnalytics {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "url_mapping_id", nullable = false)
        private UrlMapping urlMapping;

        @Column(nullable = false)
        private LocalDateTime hour;

        private int count;
}
