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
        String shippingAddress,
        String recipientName,
        String recipientPhone,
        List<OrderItemResponse> items
) {}
