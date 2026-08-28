package com.ner.smartlogix.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Common audit columns shared by the main business tables.
 *
 * <p>{@code @MappedSuperclass} means this class is NOT a table of its own: its four
 * columns are copied into every table whose entity extends it. The values are filled
 * in automatically by {@link com.ner.smartlogix.config.JpaAuditingConfig}.
 *
 * <p>High-volume tables (vehicle_location, weather_data) deliberately do NOT extend
 * this class - they already carry their own timestamp and four extra columns per row
 * would waste a lot of space.
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseAuditEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @CreatedBy
    @Column(name = "created_by", length = 50, updatable = false)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "updated_by", length = 50)
    private String updatedBy;
}
