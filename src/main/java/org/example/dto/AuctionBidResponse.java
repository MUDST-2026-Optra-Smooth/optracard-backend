package org.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Record of an individual auction bid")
public record AuctionBidResponse(
        @Schema(description = "Unique bid ID", example = "1042")
        Integer id,

        @Schema(description = "Bid amount in THB", example = "19000.00")
        Double amount,

        @Schema(description = "Anonymized bidder display identifier", example = "BIDDER-***7")
        String bidderCode,

        @Schema(description = "Timestamp when bid was submitted", example = "2026-11-16T14:22:10")
        LocalDateTime createdAt
) {}
