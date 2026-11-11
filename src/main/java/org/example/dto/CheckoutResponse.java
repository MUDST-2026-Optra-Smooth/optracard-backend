package org.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** Result of checkout. A cart with multiple sellers creates one order for each seller. */
@Schema(description = "Checkout completion result containing generated orders")
public record CheckoutResponse(
        @Schema(description = "List of generated orders (one per fulfilling merchant store)")
        List<OrderResponse> orders,

        @Schema(description = "Total number of orders created", example = "1")
        Integer orderCount,

        @Schema(description = "Combined monetary total across all orders in THB", example = "1050.00")
        Double total
) {}
