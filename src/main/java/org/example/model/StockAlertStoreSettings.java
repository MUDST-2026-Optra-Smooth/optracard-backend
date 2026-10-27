package org.example.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Threshold settings owned by one approved seller store. */
@Entity
@Table(name = "stock_alert_store_settings")
@Getter
@Setter
public class StockAlertStoreSettings {
    public static final int DEFAULT_THRESHOLD = 5;

    @Id
    @Column(name = "store_id")
    private Integer storeId;

    @Column(name = "default_threshold", nullable = false)
    private Integer defaultThreshold = DEFAULT_THRESHOLD;
}
