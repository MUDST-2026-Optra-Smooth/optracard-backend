package org.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Promotion coupon representation")
public record PromotionResponse(
        @Schema(description = "Promotion ID", example = "1")
        Integer id,

        @Schema(description = "Unique coupon code", example = "SUMMER2026")
        String code,

        @Schema(description = "Promotion title / name", example = "Summer TCG Clearance 10% Off")
        String name,

        @Schema(description = "Discount type (PERCENTAGE, FIXED_AMOUNT)", example = "PERCENTAGE")
        String discountType,

        @Schema(description = "Discount value", example = "10.0")
        Double discountValue,

        @Schema(description = "Maximum discount cap in THB", example = "500.00")
        Double maximumDiscountAmount,

        @Schema(description = "Minimum cart subtotal requirement in THB", example = "1000.00")
        Double minimumOrderAmount,

        @Schema(description = "Global usage limit", example = "100")
        Integer usageLimit,

        @Schema(description = "Per-user limit", example = "1")
        Integer perUserLimit,

        @Schema(description = "Current times redeemed", example = "24")
        Integer usageCount,

        @Schema(description = "Start timestamp", example = "2026-06-01T00:00:00")
        LocalDateTime startsAt,

        @Schema(description = "End timestamp", example = "2026-08-31T23:59:59")
        LocalDateTime endsAt,

        @Schema(description = "Active status", example = "true")
        Boolean active
) {}
