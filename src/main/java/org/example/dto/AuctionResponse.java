package org.example.dto;

import java.time.LocalDateTime;

public record AuctionResponse(
        Integer id,
        String title,
        String game,
        String type,
        String imageUrl,
        String description,
        Double startingPrice,
        Double currentPrice,
        Integer bidCount,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        LocalDateTime checkoutDeadline,
        String status,
        String source,
        Integer storeId,
        String storeName,
        Integer sellerUserId,
        Integer winningUserId,
        String reviewNote,
        LocalDateTime createdAt
) {}
