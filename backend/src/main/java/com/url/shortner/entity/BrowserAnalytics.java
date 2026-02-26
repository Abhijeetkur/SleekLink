package com.url.shortner.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "browser_analytics", indexes = {
        @Index(name = "idx_browser_url_mapping", columnList = "url_mapping_id, browser")
}, uniqueConstraints = {
        @UniqueConstraint(columnNames = { "url_mapping_id", "browser" })
})
@Getter
@Setter
public class BrowserAnalytics {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "url_mapping_id", nullable = false)
    private UrlMapping urlMapping;

    @Column(nullable = false)
    private String browser;

    private int count;
}
