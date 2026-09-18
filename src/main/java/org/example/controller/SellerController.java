package org.example.controller;

import org.example.dto.SellerOrderResponse;
import org.example.dto.CardGameOptionResponse;
import org.example.dto.SellerProductRequest;
import org.example.dto.SellerProductResponse;
import org.example.service.SellerService;
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
@RequestMapping("/api/seller")
@CrossOrigin(origins = "http://localhost:5173")
public class SellerController {
    private final SellerService sellerService;

    public SellerController(SellerService sellerService) {
        this.sellerService = sellerService;
    }

    @GetMapping("/products")
    public List<SellerProductResponse> products(Authentication authentication) {
        return sellerService.getProducts(authentication.getName());
    }

    @GetMapping("/games")
    public List<CardGameOptionResponse> games() {
        return sellerService.getCardGames();
    }

    @GetMapping("/products/{id}")
    public SellerProductResponse product(Authentication authentication, @PathVariable Integer id) {
        return sellerService.getProduct(authentication.getName(), id);
    }

    @PostMapping("/products")
    public ResponseEntity<SellerProductResponse> create(Authentication authentication,
                                                         @RequestBody SellerProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(sellerService.createProduct(authentication.getName(), request));
    }

    @PutMapping("/products/{id}")
    public SellerProductResponse update(Authentication authentication,
                                        @PathVariable Integer id,
                                        @RequestBody SellerProductRequest request) {
        return sellerService.updateProduct(authentication.getName(), id, request);
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<Void> deactivate(Authentication authentication, @PathVariable Integer id) {
        sellerService.deactivateProduct(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/orders")
    public List<SellerOrderResponse> orders(Authentication authentication) {
        return sellerService.getOrders(authentication.getName());
    }

    @PutMapping("/orders/{id}/status")
    public SellerOrderResponse updateOrderStatus(Authentication authentication,
                                                  @PathVariable Integer id,
                                                  @RequestBody StatusRequest request) {
        return sellerService.updateOrderStatus(authentication.getName(), id, request == null ? null : request.status());
    }

    public record StatusRequest(String status) {}
}
