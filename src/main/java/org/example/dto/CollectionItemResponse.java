package org.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Trading card item in a user's personal binder / collection")
public record CollectionItemResponse(
        @Schema(description = "Product ID of the card", example = "42")
        Integer id,

        @Schema(description = "Card title or product name", example = "Blue-Eyes White Dragon (Ghost Rare)")
        String name,

        @Schema(description = "Associated card game", example = "Yu-Gi-Oh!")
        String game,

        @Schema(description = "Card artwork or image URL", example = "https://images.optracard.com/cards/yugioh/bewd-ghost.jpg")
        String imageUrl,

        @Schema(description = "Total quantity owned across completed orders", example = "3")
        Long quantity
) {}
