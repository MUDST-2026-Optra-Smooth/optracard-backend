package org.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Low stock or out of stock alert")
public record StockAlertResponse(
        @Schema(description = "Alert notification ID", example = "4")
        Integer id,

        @Schema(description = "Product ID", example = "42")
        Integer productId,

        @Schema(description = "Product title", example = "Charizard VMAX Shiny")
        String productName,

        @Schema(description = "Alert classification (LOW_STOCK, OUT_OF_STOCK)", example = "LOW_STOCK")
        String alertType,

        @Schema(description = "Current available inventory units", example = "2")
        Integer stock,

        @Schema(description = "Threshold that triggered the alert", example = "5")
        Integer threshold,

        @Schema(description = "Listing origin (OFFICIAL, MARKETPLACE)", example = "MARKETPLACE")
        String listingSource,

        @Schema(description = "Merchant store ID", example = "3")
        Integer storeId,

        @Schema(description = "Merchant store name", example = "Kanto Card Shop")
        String storeName,

        @Schema(description = "Timestamp when alert was activated", example = "2026-11-10T11:00:00")
        LocalDateTime activatedAt,

        @Schema(description = "Timestamp when alert was last updated", example = "2026-11-11T09:00:00")
        LocalDateTime updatedAt,

        @Schema(description = "Whether the seller or admin has acknowledged/read the alert", example = "false")
        boolean read
) {}
