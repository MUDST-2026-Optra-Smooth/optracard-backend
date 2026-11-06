package org.example.dto;

import java.time.LocalDateTime;

public record PromotionRequest(
        String code,
        String name,
        String discountType,
        Double discountValue,
        Double maximumDiscountAmount,
        Double minimumOrderAmount,
        Integer usageLimit,
        Integer perUserLimit,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        Boolean active
) {}
