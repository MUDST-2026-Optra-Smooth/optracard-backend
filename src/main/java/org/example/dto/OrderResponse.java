package org.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Customer order details and status")
public record OrderResponse(
        @Schema(description = "Order record ID", example = "201")
        Integer orderId,

        @Schema(description = "Customer-facing unique order number", example = "ORD-20261111-9871")
        String orderNumber,

        @Schema(description = "Fulfilling store ID", example = "3")
        Integer storeId,

        @Schema(description = "Fulfilling store name", example = "Kanto Card Shop")
        String storeName,

        @Schema(description = "Inventory origin (OFFICIAL, MARKETPLACE)", example = "MARKETPLACE")
        String source,

        @Schema(description = "Order placement timestamp", example = "2026-11-11T14:30:00")
        LocalDateTime createdAt,

        @Schema(description = "Grand total amount paid in THB (subtotal - discount + shippingFee)", example = "1050.00")
        Double total,

        @Schema(description = "Payment method", example = "PROMPTPAY")
        String paymentMethod,

        @Schema(description = "Payment status (PAID, PENDING, FAILED)", example = "PAID")
        String paymentStatus,

        @Schema(description = "Order status (PENDING, PAID, PROCESSING, SHIPPED, DELIVERED, CANCELLED)", example = "PROCESSING")
        String status,

        @Schema(description = "Carrier shipping method", example = "STANDARD")
        String shippingMethod,

        @Schema(description = "Carrier shipping fee in THB", example = "50.00")
        Double shippingFee,

        @Schema(description = "Package tracking number", example = "TH1234567890")
        String trackingNumber,

        @Schema(description = "Shipping delivery address", example = "123 Blue-Eyes Blvd, Bangkok 10110")
        String shippingAddress,

        @Schema(description = "Recipient name", example = "Seto Kaiba")
        String recipientName,

        @Schema(description = "Recipient contact phone", example = "+66812345678")
        String recipientPhone,

        @Schema(description = "List of ordered items")
        List<OrderItemResponse> items,

        @Schema(description = "Items subtotal before discount and shipping", example = "1100.00")
        Double subtotal,

        @Schema(description = "Applied coupon promotion code", example = "SUMMER2026")
        String discountCode,

        @Schema(description = "Applied coupon promotion title", example = "Summer TCG Clearance 100 THB Off")
        String discountName,

        @Schema(description = "Total discount amount subtracted in THB", example = "100.00")
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
