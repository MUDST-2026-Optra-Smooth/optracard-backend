package org.example.dto;

public record CreateOrderRequest(
        String recipientName,
        String recipientPhone,
        String shippingAddress,
        String shippingMethod,
        String paymentMethod
) {}
