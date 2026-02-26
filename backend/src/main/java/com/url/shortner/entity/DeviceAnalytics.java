package com.url.shortner.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "device_analytics", indexes = {
        @Index(name = "idx_device_url_mapping", columnList = "url_mapping_id, device")
}, uniqueConstraints = {
        @UniqueConstraint(columnNames = { "url_mapping_id", "device" })
})
@Getter
@Setter
public class DeviceAnalytics {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "url_mapping_id", nullable = false)
    private UrlMapping urlMapping;

    @Column(nullable = false)
    private String device;

    private int count;
}
