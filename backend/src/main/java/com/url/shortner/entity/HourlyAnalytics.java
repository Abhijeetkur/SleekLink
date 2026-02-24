package com.url.shortner.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "hourly_analytics",
        indexes = {
                @Index(name = "idx_shortcode_hour", columnList = "shortCode, hour")
        },
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"shortCode", "hour"})
        }
)
@Getter
@Setter
public class HourlyAnalytics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String shortCode;

    @Column(nullable = false)
    private LocalDateTime hour;

    private int count;
}
