package org.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.dto.AdminData;
import org.example.service.AdminDataService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/admin")
@CrossOrigin(origins = "http://localhost:5173")
@Tag(name = "Admin Operations", description = "Administrative endpoints for platform oversight, inventory, order processing, and merchant management")
@SecurityRequirement(name = "bearerAuth")
public class AdminDataController {
    private final AdminDataService adminDataService;

    public AdminDataController(AdminDataService adminDataService) {
        this.adminDataService = adminDataService;
    }

    @Operation(
            summary = "Get admin dashboard metrics",
            description = "Retrieve summary metrics including total revenue, order count, active products, and low stock warnings."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dashboard metrics retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN authority")
    })
    @GetMapping("/dashboard")
    public AdminData.DashboardResponse dashboard() { return adminDataService.getDashboard(); }

    @Operation(
            summary = "Get supported card games",
            description = "Retrieve all card game categories configured in the platform."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Card games list retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN authority")
    })
    @GetMapping("/card-games")
    public List<AdminData.CardGameResponse> cardGames() { return adminDataService.getCardGames(); }

    @Operation(
            summary = "List official store products",
            description = "Retrieve all official Optracard inventory items including stock counts, costs, and selling prices."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Official products retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN authority")
    })
    @GetMapping("/products")
    public List<AdminData.ProductResponse> officialProducts() { return adminDataService.getOfficialProducts(); }

    @Operation(
            summary = "Get official product details",
            description = "Retrieve details of a specific official store product by ID."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product details retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN authority"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @GetMapping("/products/{id}")
    public AdminData.ProductResponse officialProduct(
            @Parameter(description = "Product ID", example = "1") @PathVariable Integer id) {
        return adminDataService.getOfficialProduct(id);
    }

    @Operation(
            summary = "Create official product",
            description = "Add a new official store product item to the catalog with approved status."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Official product created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid product payload or missing required fields"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN authority")
    })
    @PostMapping("/products")
    public ResponseEntity<AdminData.ProductResponse> createOfficialProduct(@RequestBody AdminData.ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminDataService.createOfficialProduct(request));
    }

    @Operation(
            summary = "Update official product",
            description = "Modify an existing official store product's details, pricing, stock, or metadata."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Official product updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid product update data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN authority"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @PutMapping("/products/{id}")
    public AdminData.ProductResponse updateOfficialProduct(
            @Parameter(description = "Product ID", example = "1") @PathVariable Integer id,
            @RequestBody AdminData.ProductRequest request) {
        return adminDataService.updateOfficialProduct(id, request);
    }

    @Operation(
            summary = "Toggle official product visibility",
            description = "Activate or deactivate an official product's visibility in the public storefront."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product visibility updated successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN authority"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @PutMapping("/products/{id}/active")
    public AdminData.ProductResponse updateOfficialProductActive(
            @Parameter(description = "Product ID", example = "1") @PathVariable Integer id,
            @RequestBody AdminData.ActiveRequest request) {
        return adminDataService.updateOfficialProductActive(id, request == null ? null : request.active());
    }

    @Operation(
            summary = "Delete official product",
            description = "Permanently remove an official product from the catalog and related inventories."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Product deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN authority"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @DeleteMapping("/products/{id}")
    public ResponseEntity<Void> deleteOfficialProduct(
            @Parameter(description = "Product ID", example = "1") @PathVariable Integer id) {
        adminDataService.deleteOfficialProduct(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "List customer orders",
            description = "Retrieve customer orders filterable by fulfillment source (OFFICIAL, MARKETPLACE, or ALL)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Orders list retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN authority")
    })
    @GetMapping("/orders")
    public List<AdminData.OrderResponse> orders(
            @Parameter(description = "Order source filter: OFFICIAL, MARKETPLACE, or ALL") @RequestParam(defaultValue = "OFFICIAL") String source) {
        return adminDataService.getOrders(source);
    }

    @Operation(
            summary = "Get order details",
            description = "Retrieve complete line items, tracking, shipping, and payment details for a specific order."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order details retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN authority"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    @GetMapping("/orders/{id}")
    public AdminData.OrderResponse order(
            @Parameter(description = "Order ID", example = "1") @PathVariable Integer id) {
        return adminDataService.getOrder(id);
    }

    @Operation(
            summary = "Update order status",
            description = "Update an official order's status (Processing, Shipped, Delivered) and attach shipping tracking numbers."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order status updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid status transition or missing tracking number for shipped orders"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - marketplace orders cannot be fulfilled by admin"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    @PutMapping("/orders/{id}/status")
    public AdminData.OrderResponse updateOrderStatus(
            @Parameter(description = "Order ID", example = "1") @PathVariable Integer id,
            @RequestBody AdminData.OrderStatusRequest request) {
        return adminDataService.updateOrderStatus(id, request == null ? null : request.status(),
                request == null ? null : request.trackingNumber());
    }

    @Operation(
            summary = "List registered marketplace stores",
            description = "Retrieve all merchant stores filterable by approval status (ALL, PENDING, APPROVED, REJECTED)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stores list retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN authority")
    })
    @GetMapping("/stores")
    public List<AdminData.StoreResponse> stores(
            @Parameter(description = "Filter by status: ALL, PENDING, APPROVED, REJECTED") @RequestParam(defaultValue = "ALL") String status) {
        return adminDataService.getStores(status);
    }

    @Operation(
            summary = "Get merchant store details",
            description = "Retrieve detailed information for a specific store including owner, status, and bank accounts."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Store details retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN authority"),
            @ApiResponse(responseCode = "404", description = "Store not found")
    })
    @GetMapping("/stores/{id}")
    public AdminData.StoreResponse store(
            @Parameter(description = "Store ID", example = "1") @PathVariable Integer id) {
        return adminDataService.getStore(id);
    }

    @Operation(
            summary = "List marketplace merchant products",
            description = "Retrieve all merchant-listed products across the platform or filtered by specific store ID."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Marketplace products retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN authority")
    })
    @GetMapping("/marketplace/products")
    public List<AdminData.ProductResponse> marketplaceProducts(
            @Parameter(description = "Optional store ID filter") @RequestParam(required = false) Integer storeId) {
        return adminDataService.getMarketplaceProducts(storeId);
    }

    @Operation(
            summary = "Get marketplace product details",
            description = "Retrieve detailed information for a specific merchant listing."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Marketplace product details retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN authority"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @GetMapping("/marketplace/products/{id}")
    public AdminData.ProductResponse marketplaceProduct(
            @Parameter(description = "Product ID", example = "1") @PathVariable Integer id) {
        return adminDataService.getMarketplaceProduct(id);
    }

    @Operation(
            summary = "Get smart restock predictions",
            description = "Retrieve automated restock velocity predictions and recommended restock units based on recent sales trends."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Restock predictions retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN authority")
    })
    @GetMapping("/reports/restock-predictions")
    public ResponseEntity<List<AdminData.RestockPredictionResponse>> getRestockPredictions() {
        return ResponseEntity.ok(adminDataService.getSmartRestockPredictions());
    }
}
