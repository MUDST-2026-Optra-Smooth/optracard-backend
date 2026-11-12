package org.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.dto.AuctionBidRequest;
import org.example.dto.AuctionBidResponse;
import org.example.dto.AuctionCheckoutRequest;
import org.example.dto.AuctionResponse;
import org.example.service.AuctionService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auctions")
@CrossOrigin(origins = "http://localhost:5173")
@Tag(name = "Auctions", description = "Public storefront and customer endpoints for card auctions, live bidding, and auction checkout")
public class AuctionController {
    private final AuctionService auctionService;
    private final org.example.service.AuctionPrivacyService privacy;

    public AuctionController(AuctionService auctionService, org.example.service.AuctionPrivacyService privacy) { this.auctionService = auctionService; this.privacy = privacy; }

    @Operation(
            summary = "List public auctions",
            description = "Retrieve all active or filtered public card auctions with current high bid, time remaining, and lot details."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Public auctions list retrieved successfully")
    })
    @GetMapping
    public List<AuctionResponse> list(
            Authentication authentication,
            @Parameter(description = "Filter by card game name (e.g. Pokemon, One Piece)") @RequestParam(required = false) String game,
            @Parameter(description = "Filter by source: OFFICIAL or MARKETPLACE") @RequestParam(required = false) String source,
            @Parameter(description = "Filter by status: ACTIVE, UPCOMING, ENDED") @RequestParam(required = false) String status) {
        return auctionService.listPublic(game, source, status).stream()
                .map(a -> privacy.forViewer(a, authentication == null ? null : authentication.getName())).toList();
    }

    @Operation(
            summary = "Get auction details",
            description = "Retrieve detailed information for a specific auction lot by ID, including pricing, reserve state, and winning bidder."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Auction details retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Auction not found")
    })
    @GetMapping("/mine")
    public List<AuctionResponse> mine(Authentication authentication) {
        if(authentication==null) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED);
        return auctionService.myAuctions(authentication.getName()).stream().map(a->privacy.forViewer(a,authentication.getName())).toList();
    }

    @GetMapping("/{id}")
    public AuctionResponse detail(
            Authentication authentication,
            @Parameter(description = "Auction ID", example = "1") @PathVariable Integer id) {
        return privacy.forViewer(auctionService.getPublic(id), authentication == null ? null : authentication.getName());
    }

    @Operation(
            summary = "Get auction bid history",
            description = "Retrieve the chronological history of bids placed on a specific auction lot."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bid history retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Auction not found")
    })
    @GetMapping("/{id}/bids")
    public List<AuctionBidResponse> bids(
            @Parameter(description = "Auction ID", example = "1") @PathVariable Integer id) {
        return auctionService.bidHistory(id);
    }

    @Operation(
            summary = "Place a bid on an auction",
            description = "Place a higher bid on an active auction lot. Requires customer authentication and valid bid increment.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bid placed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid bid amount, auction not active, or bid below minimum increment"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "404", description = "Auction not found")
    })
    @PostMapping("/{id}/bids")
    public AuctionBidResponse bid(
            Authentication authentication,
            @Parameter(description = "Auction ID", example = "1") @PathVariable Integer id,
            @RequestBody AuctionBidRequest request) {
        return auctionService.placeBid(authentication.getName(), id, request);
    }

    @Operation(
            summary = "Checkout won auction",
            description = "Submit shipping and payment details to checkout and claim an auction won by the authenticated user.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Auction checkout completed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid payment method, shipping details, or auction not won"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "404", description = "Auction not found")
    })
    @PostMapping("/{id}/checkout")
    public AuctionResponse checkout(
            Authentication authentication,
            @Parameter(description = "Auction ID", example = "1") @PathVariable Integer id,
            @RequestBody AuctionCheckoutRequest request) {
        return auctionService.checkout(authentication.getName(), id, request);
    }
}
