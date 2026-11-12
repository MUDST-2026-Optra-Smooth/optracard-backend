package org.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Payload for submitting or updating an auction listing")
public record AuctionRequest(
        @Schema(description = "Title of the auction listing", example = "1st Edition Shadowless Charizard Holo")
        String title,

        @Schema(description = "ID of the associated card game", example = "1")
        Integer gameId,

        @Schema(description = "Card type / rarity classification", example = "Single")
        String type,

        @Schema(description = "Card high-resolution photograph or image URL", example = "https://images.optracard.com/auctions/charizard-1st.jpg")
        String imageUrl,

        @Schema(description = "Detailed condition and grading description", example = "PSA 9 Mint condition, stored in UV-protective slab.")
        String description,

        @Schema(description = "Starting bid price in THB", example = "15000.00")
        Double startingPrice,

        @Schema(description = "Auction start timestamp (ISO-8601)", example = "2026-11-15T12:00:00")
        LocalDateTime startsAt,

        @Schema(description = "Auction closing timestamp (ISO-8601)", example = "2026-11-20T20:00:00")
        LocalDateTime endsAt
) {}
