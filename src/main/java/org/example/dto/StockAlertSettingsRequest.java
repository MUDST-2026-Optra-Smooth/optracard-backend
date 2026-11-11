package org.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request to update low stock alert threshold")
public record StockAlertSettingsRequest(
        @Schema(description = "Inventory quantity below which a low stock alert is triggered", example = "5")
        Integer defaultThreshold
) {}
