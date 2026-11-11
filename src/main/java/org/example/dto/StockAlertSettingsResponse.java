package org.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Configured low stock alert threshold settings")
public record StockAlertSettingsResponse(
        @Schema(description = "Active low stock threshold", example = "5")
        int defaultThreshold
) {}
