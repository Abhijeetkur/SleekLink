package com.url.shortner.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "daily_analytics", indexes = {
                @Index(name = "idx_daily_url_mapping_date", columnList = "url_mapping_id, date")
}, uniqueConstraints = {
                @UniqueConstraint(columnNames = { "url_mapping_id", "date" })
})
@Getter
@Setter
public class DailyAnalytics {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "url_mapping_id", nullable = false)
        private UrlMapping urlMapping;

        @Column(nullable = false)
        private LocalDate date;

        private int count;
}
