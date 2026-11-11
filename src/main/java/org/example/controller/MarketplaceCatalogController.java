package org.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.dto.MarketplaceStoreProfileResponse;
import org.example.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public storefront endpoints for approved marketplace sellers. */
@RestController
@RequestMapping("/api/marketplace")
@CrossOrigin(origins = "*")
@Tag(name = "Marketplace Storefront", description = "Public storefront endpoints for discovering verified merchant stores and vendor profiles")
public class MarketplaceCatalogController {
    private final ProductService productService;

    public MarketplaceCatalogController(ProductService productService) {
        this.productService = productService;
    }

    @Operation(
            summary = "Get marketplace store profile",
            description = "Retrieve public storefront details, merchant info, location, and verified status for an approved seller store."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Store profile retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Store not found or not approved")
    })
    @GetMapping("/stores/{storeId}")
    public ResponseEntity<MarketplaceStoreProfileResponse> store(
            @Parameter(description = "Store ID", example = "1") @PathVariable Integer storeId) {
        return productService.getMarketplaceStoreProfile(storeId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
