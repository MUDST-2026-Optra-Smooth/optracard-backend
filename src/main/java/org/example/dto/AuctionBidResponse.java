package org.example.dto;

import java.time.LocalDateTime;

public record AuctionBidResponse(
        Integer id,
        Double amount,
        String bidderCode,
        LocalDateTime createdAt
) {}
