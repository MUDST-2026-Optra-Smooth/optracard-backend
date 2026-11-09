package org.example.dto;

public record AuctionCheckoutRequest(
        String recipientName,
        String recipientPhone,
        String shippingAddress,
        String shippingMethod,
        String paymentMethod
) {}
