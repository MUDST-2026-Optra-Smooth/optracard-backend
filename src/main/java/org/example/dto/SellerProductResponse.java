package org.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Seller product listing detail and status")
public record SellerProductResponse(
        @Schema(description = "Product ID", example = "58")
        Integer id,

        @Schema(description = "Product or card name", example = "Monkey D. Luffy Manga Alternate Art")
        String name,

        @Schema(description = "Card game", example = "One Piece")
        String game,

        @Schema(description = "Card type", example = "Single")
        String type,

        @Schema(description = "Cost basis in THB", example = "12000.00")
        Double cost,

        @Schema(description = "Selling price in THB", example = "25000.00")
        Double price,

        @Schema(description = "Available inventory stock", example = "1")
        Integer stock,

        @Schema(description = "Product image URL", example = "https://images.optracard.com/seller/op05-luffy.jpg")
        String imageUrl,

        @Schema(description = "Expansion set", example = "Awakening of the New Era (OP-05)")
        String productSet,

        @Schema(description = "Card language", example = "Japanese")
        String language,

        @Schema(description = "Product description", example = "BGS 9.5 Gem Mint")
        String description,

        @Schema(description = "Admin approval status (PENDING, APPROVED, REJECTED)", example = "APPROVED")
        String approvalStatus,

        @Schema(description = "Whether listing is currently active in store", example = "true")
        Boolean active,

        @Schema(description = "Owning store ID", example = "3")
        Integer storeId,

        @Schema(description = "Owning store name", example = "Kanto Card Shop")
        String storeName
) {}
