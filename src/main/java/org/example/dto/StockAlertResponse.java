package org.example.dto;

import java.time.LocalDateTime;

public record StockAlertResponse(
        Integer id,
        Integer productId,
        String productName,
        String alertType,
        Integer stock,
        Integer threshold,
        String listingSource,
        Integer storeId,
        String storeName,
        LocalDateTime activatedAt,
        LocalDateTime updatedAt,
        boolean read
) {}
