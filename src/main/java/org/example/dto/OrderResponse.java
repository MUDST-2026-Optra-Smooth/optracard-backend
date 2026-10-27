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
        /** Items only, before discount and shipping. */
        Double subtotal,
        /** Promotion code applied at checkout, or null when none was used. */
        String discountCode,
        /** Promotion name as it was at checkout, or null when none was used. */
        String discountName,
        /** Amount taken off the items, 0 when no promotion was used. */
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
