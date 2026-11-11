package org.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Calculated preview of promotion discount applied to user cart")
public record PromotionPreviewResponse(
        @Schema(description = "Coupon code", example = "SUMMER2026")
        String code,

        @Schema(description = "Promotion title", example = "Summer TCG Clearance 10% Off")
        String name,

        @Schema(description = "Original cart subtotal in THB", example = "1500.00")
        Double subtotal,

        @Schema(description = "Calculated discount amount in THB", example = "150.00")
        Double discountAmount,

        @Schema(description = "Subtotal after applying discount in THB", example = "1350.00")
        Double discountedSubtotal,

        @Schema(description = "Discount type", example = "PERCENTAGE")
        String discountType,

        @Schema(description = "Configured discount value", example = "10.0")
        Double discountValue,

        @Schema(description = "Maximum discount cap", example = "500.00")
        Double maximumDiscountAmount,

        @Schema(description = "Minimum subtotal requirement", example = "1000.00")
        Double minimumOrderAmount,

        @Schema(description = "Per-user usage limit", example = "1")
        Integer perUserLimit
) {}
