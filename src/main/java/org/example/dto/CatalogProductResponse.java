package org.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Public card or merchandise product listing in the catalog")
public record CatalogProductResponse(
        @Schema(description = "Product ID", example = "42")
        Integer id,

        @Schema(description = "Card title or product title", example = "Charizard VMAX Shiny")
        String name,

        @Schema(description = "Card type / classification", example = "Single")
        String type,

        @Schema(description = "Card game", example = "Pokemon")
        String game,

        @Schema(description = "Selling price in THB", example = "4500.00")
        Double price,

        @Schema(description = "Remaining available stock", example = "5")
        Integer stock,

        @Schema(description = "Product image URL", example = "https://images.optracard.com/cards/charizard.jpg")
        String imageUrl,

        @Schema(description = "Product details and condition description", example = "Near Mint condition, pack fresh.")
        String description,

        @Schema(description = "Card expansion set name", example = "Shining Fates")
        String productSet,

        @Schema(description = "Card language (English, Japanese, Thai)", example = "English")
        String language,

        @Schema(description = "Inventory origin (OFFICIAL, MARKETPLACE)", example = "OFFICIAL")
        String source,

        @Schema(description = "Store details if sold by marketplace seller")
        CatalogStoreResponse store
) {}
