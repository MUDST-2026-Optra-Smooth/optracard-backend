package org.example.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** The current stock warning state for one product. */
@Entity
@Table(name = "stock_alerts")
@Getter
@Setter
public class StockAlert {
    public static final String LOW_STOCK = "LOW_STOCK";
    public static final String OUT_OF_STOCK = "OUT_OF_STOCK";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stock_alert_id")
    private Integer id;

    @Column(name = "pro_id", nullable = false, unique = true)
    private Integer productId;

    @Column(name = "alert_type", nullable = false, length = 30)
    private String alertType;

    @Column(name = "current_stock", nullable = false)
    private Integer currentStock;

    @Column(name = "threshold", nullable = false)
    private Integer threshold;

    @Column(name = "activated_at", nullable = false, updatable = false)
    private LocalDateTime activatedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (activatedAt == null) activatedAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
