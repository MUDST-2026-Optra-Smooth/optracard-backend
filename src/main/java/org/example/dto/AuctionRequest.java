package org.example.dto;

import java.time.LocalDateTime;

public record AuctionRequest(
        String title,
        Integer gameId,
        String type,
        String imageUrl,
        String description,
        Double startingPrice,
        LocalDateTime startsAt,
        LocalDateTime endsAt
) {}
