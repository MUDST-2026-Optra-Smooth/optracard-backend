package org.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Checkout information submitted by the winning auction bidder")
public record AuctionCheckoutRequest(
        @Schema(description = "Full name of delivery recipient", example = "Seto Kaiba")
        String recipientName,

        @Schema(description = "Recipient contact phone number", example = "+66812345678")
        String recipientPhone,

        @Schema(description = "Full postal delivery address", example = "123 Blue-Eyes Blvd, Bangkok 10110")
        String shippingAddress,

        @Schema(description = "Selected delivery carrier method", example = "EXPRESS")
        String shippingMethod,

        @Schema(description = "Payment method (PROMPTPAY, CREDIT_CARD, BANK_TRANSFER)", example = "PROMPTPAY")
        String paymentMethod
) {}
