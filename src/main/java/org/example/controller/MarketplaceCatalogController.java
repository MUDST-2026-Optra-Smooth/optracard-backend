package org.example.controller;

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
public class MarketplaceCatalogController {
    private final ProductService productService;

    public MarketplaceCatalogController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/stores/{storeId}")
    public ResponseEntity<MarketplaceStoreProfileResponse> store(@PathVariable Integer storeId) {
        return productService.getMarketplaceStoreProfile(storeId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
