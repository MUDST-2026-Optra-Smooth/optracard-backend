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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/auctions")
@CrossOrigin(origins = "http://localhost:5173")
@Tag(name = "Admin Auctions", description = "Administrative endpoints for managing, reviewing, creating official auctions, and cancelling listings")
@SecurityRequirement(name = "bearerAuth")
public class AdminAuctionController {
    private final AuctionService auctionService;

    public AdminAuctionController(AuctionService auctionService) { this.auctionService = auctionService; }

    @Operation(
            summary = "List auctions for admin review",
            description = "Retrieve all auctions across the platform filtered by approval status or listing state. Requires ADMIN authority."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Auctions list retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN or SUPER_ADMIN role")
    })
    @GetMapping
    public List<AuctionResponse> list(
            Authentication authentication,
            @Parameter(description = "Filter by status: ALL, PENDING, APPROVED, ACTIVE, ENDED, CANCELLED") @RequestParam(defaultValue = "ALL") String status) {
        return auctionService.adminAuctions(status);
    }

    @Operation(
            summary = "Create official auction",
            description = "Create and launch an official Optracard-hosted auction lot directly without review requirement."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Official auction created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid auction parameters"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role")
    })
    @PostMapping
    public ResponseEntity<AuctionResponse> createOfficial(Authentication authentication, @RequestBody AuctionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(auctionService.createOfficialAuction(authentication.getName(), request));
    }

    @Operation(
            summary = "Review seller auction",
            description = "Approve or reject a submitted seller auction with administrative notes."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Auction reviewed successfully"),
            @ApiResponse(responseCode = "400", description = "Auction not in pending review state"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role"),
            @ApiResponse(responseCode = "404", description = "Auction not found")
    })
    @PutMapping("/{id}/review")
    public AuctionResponse review(
            Authentication authentication,
            @Parameter(description = "Auction ID", example = "1") @PathVariable Integer id,
            @RequestBody ReviewRequest request) {
        return auctionService.reviewAuction(authentication.getName(), id, request != null && request.approved(), request == null ? null : request.note());
    }

    @Operation(
            summary = "Cancel auction by admin",
            description = "Forcefully cancel an active, pending, or scheduled auction with administrative cancellation reason."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Auction cancelled successfully"),
            @ApiResponse(responseCode = "400", description = "Cannot cancel an already completed auction"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role"),
            @ApiResponse(responseCode = "404", description = "Auction not found")
    })
    @PutMapping("/{id}/cancel")
    public AuctionResponse cancel(
            Authentication authentication,
            @Parameter(description = "Auction ID", example = "1") @PathVariable Integer id,
            @RequestBody(required = false) CancelRequest request) {
        return auctionService.cancelByAdmin(authentication.getName(), id, request == null ? null : request.note());
    }

    public record ReviewRequest(
            @Parameter(description = "Whether the auction is approved") boolean approved,
            @Parameter(description = "Feedback or review note") String note) {}

    public record CancelRequest(
            @Parameter(description = "Administrative reason for cancellation") String note) {}
}
