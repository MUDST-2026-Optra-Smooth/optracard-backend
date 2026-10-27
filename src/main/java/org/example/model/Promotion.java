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

@Entity
@Table(name = "promotions")
@Getter
@Setter
public class Promotion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "promo_id")
    private Integer promotionId;

    @Column(name = "promo_code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "promo_name", nullable = false, length = 255)
    private String name;

    @Column(name = "promo_discount_type", nullable = false, length = 20)
    private String discountType;

    @Column(name = "promo_discount_value", nullable = false)
    private Double discountValue;

    @Column(name = "promo_minimum_order", nullable = false)
    private Double minimumOrderAmount = 0d;

    @Column(name = "promo_usage_limit")
    private Integer usageLimit;

    @Column(name = "promo_usage_count", nullable = false)
    private Integer usageCount = 0;

    @Column(name = "promo_starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "promo_ends_at", nullable = false)
    private LocalDateTime endsAt;

    @Column(name = "promo_active", nullable = false)
    private Boolean active = true;
}
