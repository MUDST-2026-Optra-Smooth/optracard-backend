package org.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Representation of a live or completed card auction")
public record AuctionResponse(
        @Schema(description = "Unique auction ID", example = "501")
        Integer id,

        @Schema(description = "Auction title", example = "1st Edition Shadowless Charizard Holo")
        String title,

        @Schema(description = "Card game name", example = "Pokemon")
        String game,

        @Schema(description = "Card type classification", example = "Single")
        String type,

        @Schema(description = "Card image URL", example = "https://images.optracard.com/auctions/charizard-1st.jpg")
        String imageUrl,

        @Schema(description = "Listing description and provenance details", example = "PSA 9 Mint condition, stored in UV-protective slab.")
        String description,

        @Schema(description = "Initial starting bid in THB", example = "15000.00")
        Double startingPrice,

        @Schema(description = "Current highest bid in THB", example = "18500.00")
        Double currentPrice,

        @Schema(description = "Total number of bids placed so far", example = "7")
        Integer bidCount,

        @Schema(description = "Auction opening timestamp", example = "2026-11-15T12:00:00")
        LocalDateTime startsAt,

        @Schema(description = "Auction ending timestamp", example = "2026-11-20T20:00:00")
        LocalDateTime endsAt,

        @Schema(description = "Winner payment deadline timestamp", example = "2026-11-22T20:00:00")
        LocalDateTime checkoutDeadline,

        @Schema(description = "Lifecycle status (PENDING_APPROVAL, UPCOMING, ACTIVE, ENDED, CHECKOUT_PENDING, COMPLETED, CANCELLED)", example = "ACTIVE")
        String status,

        @Schema(description = "Listing origin (OFFICIAL, MARKETPLACE)", example = "OFFICIAL")
        String source,

        @Schema(description = "Seller store ID (null for official store)", example = "10")
        Integer storeId,

        @Schema(description = "Seller store name (null for official store)", example = "Kanto Cards")
        String storeName,

        @Schema(description = "User ID of seller", example = "2")
        Integer sellerUserId,

        @Schema(description = "User ID of winning bidder (if ended)", example = "15")
        Integer winningUserId,

        @Schema(description = "User ID of current leading bidder", example = "15")
        Integer leadingUserId,

        @Schema(description = "Admin review feedback or rejection reason", example = "Approved by catalog administrator")
        String reviewNote,

        @Schema(description = "Auction listing creation timestamp", example = "2026-11-10T08:30:00")
        LocalDateTime createdAt
) {}
