package org.example.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Append-only fee-rate history. The active rate is the newest record whose
 * effective time has passed; historical charges retain their own snapshot.
 */
@Entity
@Getter
@Setter
@Table(name = "marketplace_fee_configurations")
public class MarketplaceFeeConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fee_config_id")
    private Integer id;

    @Column(name = "fee_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal feeRate;

    @Column(name = "effective_from", nullable = false)
    private LocalDateTime effectiveFrom;

    /** Email snapshot of the super administrator who changed the rate. */
    @Column(name = "changed_by", nullable = false, length = 255)
    private String changedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (effectiveFrom == null) effectiveFrom = createdAt;
    }
}
