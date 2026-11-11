package org.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Payload for submitting an auction bid")
public record AuctionBidRequest(
        @Schema(description = "Proposed bid amount in THB (must exceed current price by minimum step)", example = "19000.00")
        Double amount
) {}
