package com.url.shortner.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "os_analytics", indexes = {
        @Index(name = "idx_os_url_mapping", columnList = "url_mapping_id, os")
}, uniqueConstraints = {
        @UniqueConstraint(columnNames = { "url_mapping_id", "os" })
})
@Getter
@Setter
public class OsAnalytics {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "url_mapping_id", nullable = false)
    private UrlMapping urlMapping;

    @Column(nullable = false)
    private String os;

    private int count;
}
