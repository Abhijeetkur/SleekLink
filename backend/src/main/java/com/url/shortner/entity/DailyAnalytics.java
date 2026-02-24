package com.url.shortner.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(
        name = "daily_analytics",
        indexes = {
                @Index(name = "idx_shortcode_date", columnList = "shortCode, date")
        },
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"shortCode", "date"})
        }
)
@Getter @Setter
public class DailyAnalytics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String shortCode;

    @Column(nullable = false)
    private LocalDate date;

    private int count;
}
