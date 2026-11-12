package org.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.dto.CartResponse;
import org.example.model.User;
import org.example.repository.UserRepository;
import org.example.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/cart")
@CrossOrigin(origins = "*")
@Tag(name = "Cart", description = "Shopping cart management endpoints for customers")
@SecurityRequirement(name = "bearerAuth")
public class CartController {

    @Autowired
    private CartService cartService;

    @Autowired
    private UserRepository userRepository;

    @Operation(
            summary = "Get user's shopping cart",
            description = "Retrieve all items currently in the authenticated user's shopping cart along with itemized totals."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cart retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - cannot access another user's cart")
    })
    @GetMapping("/{userId}")
    public ResponseEntity<CartResponse> getCart(
            @Parameter(description = "User ID", example = "1") @PathVariable Integer userId,
            Authentication authentication) {
        Integer currentUserId = authenticatedUserId(authentication, userId);
        return ResponseEntity.ok(cartService.getCartResponse(currentUserId));
    }

    @Operation(
            summary = "Add item to shopping cart",
            description = "Add a product listing with specified quantity to the customer's cart."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Item added to cart successfully"),
            @ApiResponse(responseCode = "400", description = "Requested quantity exceeds available stock or product inactive"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - cannot modify another user's cart")
    })
    @PostMapping("/add")
    public ResponseEntity<CartResponse> addItemToCart(
            @Parameter(description = "User ID", example = "1") @RequestParam Integer userId,
            @Parameter(description = "Product ID", example = "10") @RequestParam Integer productId,
            @Parameter(description = "Quantity to add", example = "1") @RequestParam Integer quantity,
            Authentication authentication) {
        
        Integer currentUserId = authenticatedUserId(authentication, userId);
        cartService.addItemToCart(currentUserId, productId, quantity);
        return ResponseEntity.ok(cartService.getCartResponse(currentUserId));
    }

    @Operation(
            summary = "Update item quantity in cart",
            description = "Modify the quantity of an item already present in the shopping cart."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Item quantity updated successfully"),
            @ApiResponse(responseCode = "400", description = "Requested quantity exceeds available stock"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - cannot modify another user's cart")
    })
    @PutMapping("/update")
    public ResponseEntity<CartResponse> updateItemQuantity(
            @Parameter(description = "User ID", example = "1") @RequestParam Integer userId,
            @Parameter(description = "Product ID", example = "10") @RequestParam Integer productId,
            @Parameter(description = "New quantity", example = "2") @RequestParam Integer quantity,
            Authentication authentication) {
        
        Integer currentUserId = authenticatedUserId(authentication, userId);
        cartService.updateItemQuantity(currentUserId, productId, quantity);
        return ResponseEntity.ok(cartService.getCartResponse(currentUserId));
    }

    @Operation(
            summary = "Remove an item from cart",
            description = "Remove a specific product listing entirely from the shopping cart."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Item removed from cart successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - cannot modify another user's cart")
    })
    @DeleteMapping("/remove")
    public ResponseEntity<CartResponse> removeItem(
            @Parameter(description = "User ID", example = "1") @RequestParam Integer userId,
            @Parameter(description = "Product ID", example = "10") @RequestParam Integer productId,
            Authentication authentication) {
        
        Integer currentUserId = authenticatedUserId(authentication, userId);
        cartService.removeItemFromCart(currentUserId, productId);
        return ResponseEntity.ok(cartService.getCartResponse(currentUserId));
    }

    @Operation(
            summary = "Clear all items from cart",
            description = "Empty all items from the customer's active shopping cart."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cart cleared successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - cannot clear another user's cart")
    })
    @DeleteMapping("/clear/{userId}")
    public ResponseEntity<CartResponse> clearCart(
            @Parameter(description = "User ID", example = "1") @PathVariable Integer userId,
            Authentication authentication) {
        Integer currentUserId = authenticatedUserId(authentication, userId);
        cartService.clearCart(currentUserId);
        return ResponseEntity.ok(cartService.getCartResponse(currentUserId));
    }

    private Integer authenticatedUserId(Authentication authentication, Integer requestedId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login required");
        }
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        if (requestedId != null && !requestedId.equals(user.getUaId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only access your own cart");
        }
        return user.getUaId();
    }
}
