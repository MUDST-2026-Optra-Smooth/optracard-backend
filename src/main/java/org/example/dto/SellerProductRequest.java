package org.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Product data submitted by an approved marketplace seller. */
@Schema(description = "Product listing payload submitted by merchant seller")
public record SellerProductRequest(
        @Schema(description = "Card title or product name", example = "Monkey D. Luffy Manga Alternate Art")
        String name,

        @Schema(description = "Card game name", example = "One Piece")
        String game,

        @Schema(description = "Card type / classification", example = "Single")
        String type,

        @Schema(description = "Seller unit cost basis in THB (for profit calculations)", example = "12000.00")
        Double cost,

        @Schema(description = "Public listing price in THB", example = "25000.00")
        Double price,

        @Schema(description = "Available physical stock count", example = "1")
        Integer stock,

        @Schema(description = "High resolution photo or scan URL", example = "https://images.optracard.com/seller/op05-luffy.jpg")
        String imageUrl,

        @Schema(description = "Card expansion set name", example = "Awakening of the New Era (OP-05)")
        String productSet,

        @Schema(description = "Card language (English, Japanese, Thai)", example = "Japanese")
        String language,

        @Schema(description = "Product details and grading condition notes", example = "BGS 9.5 Gem Mint")
        String description,

        @Schema(description = "Optional official template product ID for cross-listing", example = "10")
        Integer templateProductId
) {}
