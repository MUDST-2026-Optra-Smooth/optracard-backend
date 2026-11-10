package org.example.controller;

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
public class SellerAuctionController {
    private final AuctionService auctionService;

    public SellerAuctionController(AuctionService auctionService) { this.auctionService = auctionService; }

    @GetMapping
    public List<AuctionResponse> list(Authentication authentication) {
        return auctionService.sellerAuctions(authentication.getName());
    }

    @PostMapping
    public ResponseEntity<AuctionResponse> create(Authentication authentication, @RequestBody AuctionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(auctionService.createSellerAuction(authentication.getName(), request));
    }

    @PutMapping("/{id}")
    public AuctionResponse update(Authentication authentication, @PathVariable Integer id, @RequestBody AuctionRequest request) {
        return auctionService.updateSellerAuction(authentication.getName(), id, request);
    }

    @PostMapping("/{id}/submit")
    public AuctionResponse submit(Authentication authentication, @PathVariable Integer id) {
        return auctionService.submitSellerAuction(authentication.getName(), id);
    }

    @DeleteMapping("/{id}")
    public AuctionResponse cancel(Authentication authentication, @PathVariable Integer id) {
        return auctionService.cancelSellerAuction(authentication.getName(), id);
    }

    @PostMapping("/{id}/relist")
    public ResponseEntity<AuctionResponse> relist(Authentication authentication, @PathVariable Integer id,
                                                   @RequestBody AuctionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(auctionService.relistSellerAuction(authentication.getName(), id, request));
    }
}
