package org.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Payload to test or preview a promotion coupon against current cart")
public record PromotionCodeRequest(
        @Schema(description = "Coupon code to validate", example = "SUMMER2026")
        String code
) {}
