package org.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.dto.CheckoutResponse;
import org.example.dto.CreateOrderRequest;
import org.example.dto.OrderResponse;
import org.example.dto.PromotionCodeRequest;
import org.example.dto.PromotionPreviewResponse;
import org.example.dto.ReorderResponse;
import org.example.model.User;
import org.example.repository.UserRepository;
import org.example.service.OrderService;
import org.example.service.ReorderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "http://localhost:5173")
@Tag(name = "Orders", description = "Customer order placement, checkout, order history, promotion preview, and reorder endpoints")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {
    private final OrderService orderService;
    private final ReorderService reorderService;
    private final UserRepository userRepository;

    public OrderController(OrderService orderService, ReorderService reorderService, UserRepository userRepository) {
        this.orderService = orderService;
        this.reorderService = reorderService;
        this.userRepository = userRepository;
    }

    @Operation(
            summary = "Get user order history",
            description = "Retrieve list of past orders placed by the authenticated customer."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order history retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required")
    })
    @GetMapping
    public ResponseEntity<List<OrderResponse>> history(Authentication authentication) {
        return ResponseEntity.ok(orderService.getOrderHistory(currentUser(authentication).getUaId()));
    }

    @Operation(
            summary = "Get order details by order number",
            description = "Retrieve complete line items, tracking numbers, shipping fee, and delivery address for a specific order."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order details retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - cannot access another user's order"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    @GetMapping("/{orderNumber}")
    public ResponseEntity<OrderResponse> detail(
            Authentication authentication,
            @Parameter(description = "Unique order number", example = "ORD-20261101-0001") @PathVariable String orderNumber) {
        return ResponseEntity.ok(orderService.getOrderDetail(currentUser(authentication).getUaId(), orderNumber));
    }

    @Operation(
            summary = "Place order from user's current cart",
            description = "Checkout items in customer cart, validate stock availability, apply promotion coupon, decrement inventory, and generate order records."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order placed successfully"),
            @ApiResponse(responseCode = "400", description = "Insufficient inventory, empty cart, or invalid payment/shipping method"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required")
    })
    @PostMapping
    public ResponseEntity<CheckoutResponse> placeOrder(
            Authentication authentication,
            @RequestBody CreateOrderRequest request) {
        return ResponseEntity.ok(orderService.placeOrder(currentUser(authentication).getUaId(), request));
    }

    @Operation(
            summary = "Preview promotion discount",
            description = "Calculate discount amount and discounted subtotal for a promotional coupon code against the customer's current cart items."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Promotion preview calculated successfully"),
            @ApiResponse(responseCode = "400", description = "Expired, invalid, or minimum-order unmet coupon code"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required")
    })
    @PostMapping("/promotion-preview")
    public ResponseEntity<PromotionPreviewResponse> previewPromotion(
            Authentication authentication,
            @RequestBody PromotionCodeRequest request) {
        return ResponseEntity.ok(orderService.previewPromotion(currentUser(authentication).getUaId(), request));
    }

    @Operation(
            summary = "Reorder items from a past order",
            description = "Add the items of a past order back into the user's cart. Items that are deleted, inactive or out of stock are reported in the response instead of being added."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reorder processed and available items added to cart"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "404", description = "Past order not found")
    })
    @PostMapping("/{orderNumber}/reorder")
    public ResponseEntity<ReorderResponse> reorder(
            Authentication authentication,
            @Parameter(description = "Order number of past order", example = "ORD-20261101-0001") @PathVariable String orderNumber) {
        return ResponseEntity.ok(reorderService.reorder(currentUser(authentication).getUaId(), orderNumber));
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
    }
}
