package org.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Ordered line item snapshot")
public record OrderItemResponse(
        @Schema(description = "Product ID", example = "42")
        Integer productId,

        @Schema(description = "Product or card title", example = "Charizard VMAX Shiny")
        String name,

        @Schema(description = "Card game", example = "Pokemon")
        String game,

        @Schema(description = "Product image URL", example = "https://images.optracard.com/cards/charizard.jpg")
        String imageUrl,

        @Schema(description = "Purchased unit price in THB", example = "550.00")
        Double price,

        @Schema(description = "Quantity purchased", example = "2")
        Integer quantity,

        @Schema(description = "Fulfilling store name", example = "Kanto Card Shop")
        String storeName
) {}
