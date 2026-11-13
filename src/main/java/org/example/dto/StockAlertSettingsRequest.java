package org.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request to update low stock alert threshold")
public record StockAlertSettingsRequest(
        @Schema(description = "Maximum inventory quantity that triggers a low stock alert", example = "5")
        Integer defaultThreshold
) {}
