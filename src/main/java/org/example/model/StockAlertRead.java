package org.example.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** Per-user read state for a stock alert. */
@Entity
@Table(name = "stock_alert_reads")
@Getter
@Setter
public class StockAlertRead {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stock_alert_read_id")
    private Integer id;

    @Column(name = "stock_alert_id", nullable = false)
    private Integer alertId;

    @Column(name = "ua_id", nullable = false)
    private Integer userId;

    @Column(name = "read_at", nullable = false)
    private LocalDateTime readAt;
}
