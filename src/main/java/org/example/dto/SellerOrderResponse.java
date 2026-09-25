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
        List<OrderItemResponse> items
) {}
