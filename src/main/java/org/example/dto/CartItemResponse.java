package org.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Line item inside a shopping cart")
public record CartItemResponse(
        @Schema(description = "Product ID", example = "42")
        Integer productId,

        @Schema(description = "Quantity of this item in the cart", example = "2")
        Integer quantity,

        @Schema(description = "Card or product title", example = "Pikachu Illustrator Promo")
        String name,

        @Schema(description = "Card game", example = "Pokemon")
        String game,

        @Schema(description = "Product type / category", example = "Single")
        String category,

        @Schema(description = "Unit price in THB", example = "500.00")
        Double price,

        @Schema(description = "Available inventory in stock", example = "10")
        Integer stock,

        @Schema(description = "Product image URL", example = "https://images.optracard.com/cards/pikachu.jpg")
        String imageUrl,

        @Schema(description = "Fulfilling store ID (null for official store)", example = "3")
        Integer storeId,

        @Schema(description = "Fulfilling store name", example = "Kanto Card Shop")
        String storeName,

        @Schema(description = "Inventory origin (OFFICIAL, MARKETPLACE)", example = "MARKETPLACE")
        String source
) {}
