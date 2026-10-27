package org.example.dto;

import java.time.LocalDateTime;

public record PromotionResponse(
        Integer id,
        String code,
        String name,
        String discountType,
        Double discountValue,
        Double minimumOrderAmount,
        Integer usageLimit,
        Integer usageCount,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        Boolean active
) {}
