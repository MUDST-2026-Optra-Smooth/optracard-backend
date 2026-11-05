package org.example.dto;

import java.time.LocalDateTime;

public record PromotionResponse(
        Integer id,
        String code,
        String name,
        String discountType,
        Double discountValue,
        Double maximumDiscountAmount,
        Double minimumOrderAmount,
        Integer usageLimit,
        Integer perUserLimit,
        Integer usageCount,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        Boolean active
) {}
