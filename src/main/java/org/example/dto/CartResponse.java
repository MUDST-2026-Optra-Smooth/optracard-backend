package org.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "User shopping cart summary and contents")
public record CartResponse(
        @Schema(description = "Cart identifier", example = "12")
        Integer cartId,

        @Schema(description = "Line items in the cart")
        List<CartItemResponse> items,

        @Schema(description = "Subtotal monetary value before discounts or shipping", example = "1250.00")
        Double subtotal,

        @Schema(description = "Total number of individual item units", example = "3")
        Integer totalItemCount
) {}
