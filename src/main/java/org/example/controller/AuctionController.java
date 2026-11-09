package org.example.controller;

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
public class AuctionController {
    private final AuctionService auctionService;

    public AuctionController(AuctionService auctionService) { this.auctionService = auctionService; }

    @GetMapping
    public List<AuctionResponse> list(@RequestParam(required = false) String game,
                                      @RequestParam(required = false) String source,
                                      @RequestParam(required = false) String status) {
        return auctionService.listPublic(game, source, status);
    }

    @GetMapping("/{id}")
    public AuctionResponse detail(@PathVariable Integer id) { return auctionService.getPublic(id); }

    @GetMapping("/{id}/bids")
    public List<AuctionBidResponse> bids(@PathVariable Integer id) { return auctionService.bidHistory(id); }

    @PostMapping("/{id}/bids")
    public AuctionBidResponse bid(Authentication authentication, @PathVariable Integer id,
                                  @RequestBody AuctionBidRequest request) {
        return auctionService.placeBid(authentication.getName(), id, request);
    }

    @PostMapping("/{id}/checkout")
    public AuctionResponse checkout(Authentication authentication, @PathVariable Integer id,
                                    @RequestBody AuctionCheckoutRequest request) {
        return auctionService.checkout(authentication.getName(), id, request);
    }
}
