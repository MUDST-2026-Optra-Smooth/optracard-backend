package org.example.controller;

import org.example.dto.StoreApplicationRequest;
import org.example.dto.StoreApplicationResponse;
import org.example.service.MarketplaceStoreService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stores")
@CrossOrigin(origins = "http://localhost:5173")
public class MarketplaceStoreController {
    private final MarketplaceStoreService storeService;

    public MarketplaceStoreController(MarketplaceStoreService storeService) {
        this.storeService = storeService;
    }

    @GetMapping("/my")
    public ResponseEntity<StoreApplicationResponse> getMyStore(Authentication authentication) {
        StoreApplicationResponse response = storeService.getMyStore(authentication.getName());
        return response == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(response);
    }

    @PostMapping("/my")
    public ResponseEntity<StoreApplicationResponse> submit(
            Authentication authentication, @RequestBody StoreApplicationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(storeService.saveApplication(authentication.getName(), request));
    }

    @PutMapping("/my")
    public ResponseEntity<StoreApplicationResponse> update(
            Authentication authentication, @RequestBody StoreApplicationRequest request) {
        return ResponseEntity.ok(storeService.saveApplication(authentication.getName(), request));
    }
}
