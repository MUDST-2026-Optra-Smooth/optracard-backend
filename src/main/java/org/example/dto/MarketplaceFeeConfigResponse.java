package org.example.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MarketplaceFeeConfigResponse(
        Integer id,
        BigDecimal feeRate,
        LocalDateTime effectiveFrom,
        String changedBy,
        LocalDateTime createdAt
) {}
