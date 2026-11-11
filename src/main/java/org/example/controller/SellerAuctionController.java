package org.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.dto.AuctionRequest;
import org.example.dto.AuctionResponse;
import org.example.service.AuctionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/seller/auctions")
@CrossOrigin(origins = "http://localhost:5173")
@Tag(name = "Seller Auctions", description = "Merchant endpoints for creating, managing, relisting, and submitting seller card auctions")
@SecurityRequirement(name = "bearerAuth")
public class SellerAuctionController {
    private final AuctionService auctionService;

    public SellerAuctionController(AuctionService auctionService) { this.auctionService = auctionService; }

    @Operation(
            summary = "List seller auctions",
            description = "Retrieve all auctions owned by the authenticated seller's store across all lifecycle states."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Seller auctions list retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires SELLER role")
    })
    @GetMapping
    public List<AuctionResponse> list(Authentication authentication) {
        return auctionService.sellerAuctions(authentication.getName());
    }

    @Operation(
            summary = "Create seller auction draft",
            description = "Create a new draft auction lot for the seller's approved marketplace store."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Auction draft created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid auction parameters, starting bid, or duration"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires SELLER role")
    })
    @PostMapping
    public ResponseEntity<AuctionResponse> create(Authentication authentication, @RequestBody AuctionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(auctionService.createSellerAuction(authentication.getName(), request));
    }

    @Operation(
            summary = "Update seller auction draft",
            description = "Update details, reserve price, or scheduling for a draft or unapproved seller auction."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Auction draft updated successfully"),
            @ApiResponse(responseCode = "400", description = "Cannot update an active or locked auction"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "404", description = "Auction not found")
    })
    @PutMapping("/{id}")
    public AuctionResponse update(
            Authentication authentication,
            @Parameter(description = "Auction ID", example = "1") @PathVariable Integer id,
            @RequestBody AuctionRequest request) {
        return auctionService.updateSellerAuction(authentication.getName(), id, request);
    }

    @Operation(
            summary = "Submit auction for review",
            description = "Submit a draft auction to platform administrators for approval before going live."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Auction submitted for review successfully"),
            @ApiResponse(responseCode = "400", description = "Auction is not in a submittable state"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "404", description = "Auction not found")
    })
    @PostMapping("/{id}/submit")
    public AuctionResponse submit(
            Authentication authentication,
            @Parameter(description = "Auction ID", example = "1") @PathVariable Integer id) {
        return auctionService.submitSellerAuction(authentication.getName(), id);
    }

    @Operation(
            summary = "Cancel seller auction",
            description = "Cancel or withdraw a draft or pending auction before bidding commences."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Auction cancelled successfully"),
            @ApiResponse(responseCode = "400", description = "Cannot cancel an auction with active bids"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "404", description = "Auction not found")
    })
    @DeleteMapping("/{id}")
    public AuctionResponse cancel(
            Authentication authentication,
            @Parameter(description = "Auction ID", example = "1") @PathVariable Integer id) {
        return auctionService.cancelSellerAuction(authentication.getName(), id);
    }

    @Operation(
            summary = "Relist ended auction",
            description = "Create a new auction listing copy from an ended, expired, or unsold lot."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Auction relisted successfully"),
            @ApiResponse(responseCode = "400", description = "Auction cannot be relisted"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "404", description = "Auction not found")
    })
    @PostMapping("/{id}/relist")
    public ResponseEntity<AuctionResponse> relist(
            Authentication authentication,
            @Parameter(description = "Auction ID", example = "1") @PathVariable Integer id,
            @RequestBody AuctionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(auctionService.relistSellerAuction(authentication.getName(), id, request));
    }
}
