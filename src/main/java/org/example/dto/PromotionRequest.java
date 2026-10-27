package org.example.dto;

import java.time.LocalDateTime;

public record PromotionRequest(
        String code,
        String name,
        String discountType,
        Double discountValue,
        Double minimumOrderAmount,
        Integer usageLimit,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        Boolean active
) {}
