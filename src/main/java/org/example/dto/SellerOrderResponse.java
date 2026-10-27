package org.example.dto;

import java.time.LocalDateTime;
import java.util.List;

public record SellerOrderResponse(
        Integer orderId,
        String orderNumber,
        Integer buyerId,
        LocalDateTime createdAt,
        Double total,
        String status,
        String paymentStatus,
        String shippingMethod,
        String shippingAddress,
        String recipientName,
        String recipientPhone,
        String trackingNumber,
        List<OrderItemResponse> items,
        /** Store name as it was at checkout. */
        String storeName,
        Double shippingFee,
        /** Items only, before discount and shipping. */
        Double subtotal,
        String discountCode,
        String discountName,
        Double discountAmount
) {}
