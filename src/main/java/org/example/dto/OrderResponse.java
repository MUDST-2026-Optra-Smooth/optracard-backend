package org.example.dto;

import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Integer orderId,
        String orderNumber,
        Integer storeId,
        String storeName,
        String source,
        LocalDateTime createdAt,
        Double total,
        String paymentMethod,
        String paymentStatus,
        String status,
        String shippingMethod,
        Double shippingFee,
        String trackingNumber,
        String shippingAddress,
        String recipientName,
        String recipientPhone,
        List<OrderItemResponse> items,
        Double subtotal,
        String promotionCode,
        String promotionName,
        Double discountAmount
) {
    public OrderResponse(Integer orderId, String orderNumber, Integer storeId, String storeName, String source,
                         LocalDateTime createdAt, Double total, String paymentMethod, String paymentStatus,
                         String status, String shippingMethod, Double shippingFee, String trackingNumber,
                         String shippingAddress, String recipientName, String recipientPhone,
                         List<OrderItemResponse> items) {
        this(orderId, orderNumber, storeId, storeName, source, createdAt, total, paymentMethod, paymentStatus,
                status, shippingMethod, shippingFee, trackingNumber, shippingAddress, recipientName, recipientPhone,
                items, null, null, null, 0d);
    }
}
