package org.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Seller merchant view of an assigned customer order")
public record SellerOrderResponse(
        @Schema(description = "Order record ID", example = "201")
        Integer orderId,

        @Schema(description = "Order number", example = "ORD-20261111-9871")
        String orderNumber,

        @Schema(description = "Buyer user ID", example = "101")
        Integer buyerId,

        @Schema(description = "Creation timestamp", example = "2026-11-11T14:30:00")
        LocalDateTime createdAt,

        @Schema(description = "Net total for this store in THB", example = "1050.00")
        Double total,

        @Schema(description = "Fulfillment status (PENDING, PAID, PROCESSING, SHIPPED, DELIVERED, CANCELLED)", example = "PROCESSING")
        String status,

        @Schema(description = "Payment status", example = "PAID")
        String paymentStatus,

        @Schema(description = "Carrier shipping method", example = "STANDARD")
        String shippingMethod,

        @Schema(description = "Buyer delivery address", example = "123 Blue-Eyes Blvd, Bangkok 10110")
        String shippingAddress,

        @Schema(description = "Recipient contact name", example = "Seto Kaiba")
        String recipientName,

        @Schema(description = "Recipient phone number", example = "+66812345678")
        String recipientPhone,

        @Schema(description = "Carrier package tracking number", example = "TH1234567890")
        String trackingNumber,

        @Schema(description = "Ordered items assigned to this seller")
        List<OrderItemResponse> items,

        @Schema(description = "Store name as it was at checkout", example = "Kanto Card Shop")
        String storeName,

        @Schema(description = "Shipping fee allocated to this order", example = "50.00")
        Double shippingFee,

        @Schema(description = "Items subtotal before discount and shipping", example = "1100.00")
        Double subtotal,

        @Schema(description = "Applied coupon promotion code", example = "SUMMER2026")
        String discountCode,

        @Schema(description = "Applied coupon promotion name", example = "Summer TCG Clearance 100 THB Off")
        String discountName,

        @Schema(description = "Discount amount deducted", example = "100.00")
        Double discountAmount
) {}
