package org.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.dto.CardGameOptionResponse;
import org.example.dto.SellerOrderResponse;
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
@Tag(name = "Seller Portal", description = "Merchant portal endpoints for inventory management, product listings, order fulfillment, and tracking")
@SecurityRequirement(name = "bearerAuth")
public class SellerController {
    private final SellerService sellerService;

    public SellerController(SellerService sellerService) {
        this.sellerService = sellerService;
    }

    @Operation(
            summary = "List seller store products",
            description = "Retrieve all product listings belonging to the authenticated seller's store, including approval status and active flags."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Seller products list retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires SELLER role")
    })
    @GetMapping("/products")
    public List<SellerProductResponse> products(Authentication authentication) {
        return sellerService.getProducts(authentication.getName());
    }

    @Operation(
            summary = "Get available card game categories",
            description = "Retrieve the list of supported card games to categorize new seller listings."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Card game categories retrieved successfully")
    })
    @GetMapping("/games")
    public List<CardGameOptionResponse> games() {
        return sellerService.getCardGames();
    }

    @Operation(
            summary = "Get seller product by ID",
            description = "Retrieve details of a specific product listing owned by the authenticated seller."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product details retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires SELLER role or store ownership"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @GetMapping("/products/{id}")
    public SellerProductResponse product(
            Authentication authentication,
            @Parameter(description = "Product ID", example = "1") @PathVariable Integer id) {
        return sellerService.getProduct(authentication.getName(), id);
    }

    @Operation(
            summary = "Create marketplace product listing",
            description = "Create a new product listing under the authenticated seller's store. Starts in PENDING review state."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Product listing created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid product parameters or unverified store"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires approved seller status")
    })
    @PostMapping("/products")
    public ResponseEntity<SellerProductResponse> create(
            Authentication authentication,
            @RequestBody SellerProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(sellerService.createProduct(authentication.getName(), request));
    }

    @Operation(
            summary = "Update marketplace product listing",
            description = "Update pricing, description, set, or stock for an existing seller product listing."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid update parameters"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - not the store owner"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @PutMapping("/products/{id}")
    public SellerProductResponse update(
            Authentication authentication,
            @Parameter(description = "Product ID", example = "1") @PathVariable Integer id,
            @RequestBody SellerProductRequest request) {
        return sellerService.updateProduct(authentication.getName(), id, request);
    }

    @Operation(
            summary = "Toggle product active state",
            description = "Enable or disable visibility of an approved seller product in the marketplace catalog."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product active state updated successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - not the store owner"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @PutMapping("/products/{id}/active")
    public SellerProductResponse updateProductActive(
            Authentication authentication,
            @Parameter(description = "Product ID", example = "1") @PathVariable Integer id,
            @RequestBody ActiveRequest request) {
        return sellerService.updateProductActive(authentication.getName(), id,
                request == null ? null : request.active());
    }

    @Operation(
            summary = "Delete seller product",
            description = "Permanently delete an unapproved or removed product listing from the seller's inventory."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Product deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - not the store owner"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @DeleteMapping("/products/{id}")
    public ResponseEntity<Void> delete(
            Authentication authentication,
            @Parameter(description = "Product ID", example = "1") @PathVariable Integer id) {
        sellerService.deleteProduct(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "List seller orders",
            description = "Retrieve all customer orders containing items from the authenticated seller's store."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Seller orders retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires SELLER role")
    })
    @GetMapping("/orders")
    public List<SellerOrderResponse> orders(Authentication authentication) {
        return sellerService.getOrders(authentication.getName());
    }

    @Operation(
            summary = "Get seller order details",
            description = "Retrieve order line items, buyer delivery address, and tracking info for an order containing the seller's goods."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Seller order details retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - order does not contain seller's products"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    @GetMapping("/orders/{id}")
    public SellerOrderResponse order(
            Authentication authentication,
            @Parameter(description = "Order ID", example = "1") @PathVariable Integer id) {
        return sellerService.getOrder(authentication.getName(), id);
    }

    @Operation(
            summary = "Update seller order fulfillment status",
            description = "Advance order status (e.g. Processing -> Shipped) and attach carrier tracking number."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order status updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid status transition or missing tracking number when shipping"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - order does not belong to seller"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    @PutMapping("/orders/{id}/status")
    public SellerOrderResponse updateOrderStatus(
            Authentication authentication,
            @Parameter(description = "Order ID", example = "1") @PathVariable Integer id,
            @RequestBody StatusRequest request) {
        return sellerService.updateOrderStatus(authentication.getName(), id, request == null ? null : request.status(),
                request == null ? null : request.trackingNumber());
    }

    public record StatusRequest(
            @Parameter(description = "New order status: Processing, Shipped, Delivered") String status,
            @Parameter(description = "Carrier tracking number required when shipping") String trackingNumber) {}

    public record ActiveRequest(
            @Parameter(description = "Active visibility state in catalog") Boolean active) {}
}
