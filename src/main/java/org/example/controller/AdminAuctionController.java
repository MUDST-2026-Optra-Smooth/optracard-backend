package org.example.controller;

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
public class AdminAuctionController {
    private final AuctionService auctionService;

    public AdminAuctionController(AuctionService auctionService) { this.auctionService = auctionService; }

    @GetMapping
    public List<AuctionResponse> list(Authentication authentication, @RequestParam(defaultValue = "ALL") String status) {
        return auctionService.adminAuctions(status);
    }

    @PostMapping
    public ResponseEntity<AuctionResponse> createOfficial(Authentication authentication, @RequestBody AuctionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(auctionService.createOfficialAuction(authentication.getName(), request));
    }

    @PutMapping("/{id}/review")
    public AuctionResponse review(Authentication authentication, @PathVariable Integer id, @RequestBody ReviewRequest request) {
        return auctionService.reviewAuction(authentication.getName(), id, request != null && request.approved(), request == null ? null : request.note());
    }

    @PutMapping("/{id}/cancel")
    public AuctionResponse cancel(Authentication authentication, @PathVariable Integer id, @RequestBody(required = false) CancelRequest request) {
        return auctionService.cancelByAdmin(authentication.getName(), id, request == null ? null : request.note());
    }

    public record ReviewRequest(boolean approved, String note) {}
    public record CancelRequest(String note) {}
}
