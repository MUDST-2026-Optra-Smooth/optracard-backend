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

import java.time.LocalDateTime;

/** A delivery update shown only to the customer who placed the order. */
@Entity
@Table(name = "order_notifications")
@Getter
@Setter
public class OrderNotification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Integer id;

    @Column(name = "ua_id", nullable = false)
    private Integer userId;

    @Column(name = "ord_id", nullable = false)
    private Integer orderId;

    @Column(name = "notification_order_number", nullable = false, length = 50)
    private String orderNumber;

    @Column(name = "notification_message", nullable = false, length = 500)
    private String message;

    @Column(name = "notification_tracking_number", nullable = false, length = 100)
    private String trackingNumber;

    @Column(name = "notification_created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "notification_read_at")
    private LocalDateTime readAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
