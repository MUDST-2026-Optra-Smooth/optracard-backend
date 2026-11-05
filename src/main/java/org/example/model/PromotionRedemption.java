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

/** A checkout-level record used to enforce a promotion's per-customer limit. */
@Entity
@Table(name = "promotion_redemptions")
@Getter
@Setter
public class PromotionRedemption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "redemption_id")
    private Integer redemptionId;

    @Column(name = "promo_id", nullable = false)
    private Integer promotionId;

    @Column(name = "ua_id", nullable = false)
    private Integer userId;

    @Column(name = "redemption_created_at", nullable = false)
    private LocalDateTime createdAt;

    public PromotionRedemption() {
        this.createdAt = LocalDateTime.now();
    }
}
