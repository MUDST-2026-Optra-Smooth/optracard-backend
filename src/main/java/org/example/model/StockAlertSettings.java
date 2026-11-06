package org.example.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Singleton system settings for stock warnings. */
@Entity
@Table(name = "stock_alert_settings")
@Getter
@Setter
public class StockAlertSettings {
    public static final int SETTINGS_ID = 1;
    public static final int DEFAULT_THRESHOLD = 5;

    @Id
    @Column(name = "setting_id")
    private Integer id = SETTINGS_ID;

    @Column(name = "default_threshold", nullable = false)
    private Integer defaultThreshold = DEFAULT_THRESHOLD;
}
