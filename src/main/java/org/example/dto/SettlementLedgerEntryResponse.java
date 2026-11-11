package org.example.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SettlementLedgerEntryResponse(
        Integer id,
        String type,
        String status,
        Integer sellerUserId,
        Integer storeId,
        String storeName,
        String referenceType,
        String referenceId,
        String referenceNumber,
        BigDecimal grossAmount,
        BigDecimal feeRate,
        BigDecimal feeAmount,
        BigDecimal netAmount,
        Integer reversalOfId,
        LocalDateTime createdAt
) {}
