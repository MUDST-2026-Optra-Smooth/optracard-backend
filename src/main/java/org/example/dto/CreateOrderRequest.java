package org.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Checkout information to create an order from current cart")
public record CreateOrderRequest(
        @Schema(description = "Recipient full legal name", example = "Seto Kaiba")
        String recipientName,

        @Schema(description = "Recipient contact phone number", example = "+66812345678")
        String recipientPhone,

        @Schema(description = "Shipping delivery address", example = "123 Blue-Eyes Blvd, Bangkok 10110")
        String shippingAddress,

        @Schema(description = "Shipping method (STANDARD, EXPRESS)", example = "STANDARD")
        String shippingMethod,

        @Schema(description = "Payment method (PROMPTPAY, CREDIT_CARD, BANK_TRANSFER)", example = "PROMPTPAY")
        String paymentMethod,

        @Schema(description = "Optional promotion coupon code", example = "SUMMER2026")
        String promotionCode
) {
    public CreateOrderRequest(String recipientName, String recipientPhone, String shippingAddress,
                              String shippingMethod, String paymentMethod) {
        this(recipientName, recipientPhone, shippingAddress, shippingMethod, paymentMethod, null);
    }
}
