package org.example.dto;

public record CreateOrderRequest(
        String recipientName,
        String recipientPhone,
        String shippingAddress,
        String shippingMethod,
        String paymentMethod,
        String promotionCode
) {
    public CreateOrderRequest(String recipientName, String recipientPhone, String shippingAddress,
                              String shippingMethod, String paymentMethod) {
        this(recipientName, recipientPhone, shippingAddress, shippingMethod, paymentMethod, null);
    }
}
