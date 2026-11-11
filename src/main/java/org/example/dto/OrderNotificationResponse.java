package org.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Customer in-app notification regarding order fulfillment and tracking updates")
public record OrderNotificationResponse(
        @Schema(description = "Notification record ID", example = "10")
        Integer id,

        @Schema(description = "Associated order ID", example = "201")
        Integer orderId,

        @Schema(description = "Associated order number", example = "ORD-20261111-9871")
        String orderNumber,

        @Schema(description = "Notification message text", example = "Your order ORD-20261111-9871 has been shipped via STANDARD.")
        String message,

        @Schema(description = "Package tracking number", example = "TH1234567890")
        String trackingNumber,

        @Schema(description = "Notification timestamp", example = "2026-11-11T16:00:00")
        LocalDateTime createdAt,

        @Schema(description = "Whether the notification has been marked as read", example = "false")
        boolean read
) {}
