package org.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Marketplace Store Applications", description = "Endpoints for submitting and viewing merchant store onboarding applications")
@SecurityRequirement(name = "bearerAuth")
public class MarketplaceStoreController {
    private final MarketplaceStoreService storeService;

    public MarketplaceStoreController(MarketplaceStoreService storeService) {
        this.storeService = storeService;
    }

    @Operation(
            summary = "Get current user's store application",
            description = "Retrieve the merchant store application status, verification state, and details for the authenticated user."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Store application retrieved successfully"),
            @ApiResponse(responseCode = "204", description = "No existing store application found for this user"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required")
    })
    @GetMapping("/my")
    public ResponseEntity<StoreApplicationResponse> getMyStore(Authentication authentication) {
        StoreApplicationResponse response = storeService.getMyStore(authentication.getName());
        return response == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Submit a new merchant store application",
            description = "Submit merchant business registration details, store name, slug, description, and bank account for approval."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Store application submitted successfully in PENDING status"),
            @ApiResponse(responseCode = "400", description = "Duplicate store slug or missing required fields"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required")
    })
    @PostMapping("/my")
    public ResponseEntity<StoreApplicationResponse> submit(
            Authentication authentication, @RequestBody StoreApplicationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(storeService.saveApplication(authentication.getName(), request));
    }

    @Operation(
            summary = "Update store application",
            description = "Update details of a pending or rejected store onboarding application."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Store application updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid store details or duplicate slug"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required")
    })
    @PutMapping("/my")
    public ResponseEntity<StoreApplicationResponse> update(
            Authentication authentication, @RequestBody StoreApplicationRequest request) {
        return ResponseEntity.ok(storeService.saveApplication(authentication.getName(), request));
    }
}
