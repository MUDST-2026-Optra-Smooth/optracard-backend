package org.example.dto;

import java.time.LocalDateTime;

public record OrderNotificationResponse(
        Integer id,
        Integer orderId,
        String orderNumber,
        String message,
        String trackingNumber,
        LocalDateTime createdAt,
        boolean read
) {}
