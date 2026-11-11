package org.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Payload to create or update a promotion discount coupon")
public record PromotionRequest(
        @Schema(description = "Unique uppercase coupon code", example = "SUMMER2026")
        String code,

        @Schema(description = "User-facing title / description", example = "Summer TCG Clearance 10% Off")
        String name,

        @Schema(description = "Discount calculation type (PERCENTAGE, FIXED_AMOUNT)", example = "PERCENTAGE")
        String discountType,

        @Schema(description = "Discount value (percentage 1-100 or fixed THB amount)", example = "10.0")
        Double discountValue,

        @Schema(description = "Maximum discount cap in THB for percentage discounts", example = "500.00")
        Double maximumDiscountAmount,

        @Schema(description = "Minimum cart subtotal required to apply coupon", example = "1000.00")
        Double minimumOrderAmount,

        @Schema(description = "Global coupon redemption limit across all users", example = "100")
        Integer usageLimit,

        @Schema(description = "Maximum redemption limit per user account", example = "1")
        Integer perUserLimit,

        @Schema(description = "Promotion validity start timestamp", example = "2026-06-01T00:00:00")
        LocalDateTime startsAt,

        @Schema(description = "Promotion expiration timestamp", example = "2026-08-31T23:59:59")
        LocalDateTime endsAt,

        @Schema(description = "Whether coupon is active and redeemable", example = "true")
        Boolean active
) {}
