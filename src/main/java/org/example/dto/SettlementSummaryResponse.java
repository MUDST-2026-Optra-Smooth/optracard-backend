package org.example.dto;

import java.math.BigDecimal;

public record SettlementSummaryResponse(
        BigDecimal grossAmount,
        BigDecimal feeAmount,
        BigDecimal netAmount
) {}
