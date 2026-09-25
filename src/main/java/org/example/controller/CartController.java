package org.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.dto.CartResponse;
import org.example.service.CartService;
import org.example.model.User;
import org.example.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
@CrossOrigin(origins = "*")
@Tag(name = "Cart", description = "Shopping cart management endpoints")
public class CartController {

    @Autowired
    private CartService cartService;

    @Autowired
    private UserRepository userRepository;

    @Operation(summary = "Get user's cart")
    @GetMapping("/{userId}")
    public ResponseEntity<CartResponse> getCart(@PathVariable Integer userId, Authentication authentication) {
        Integer currentUserId = authenticatedUserId(authentication, userId);
        return ResponseEntity.ok(cartService.getCartResponse(currentUserId));
    }

    @Operation(summary = "Add item to cart")
    @PostMapping("/add")
    public ResponseEntity<CartResponse> addItemToCart(
            @RequestParam Integer userId,
            @RequestParam Integer productId,
            @RequestParam Integer quantity,
            Authentication authentication) {
        
        Integer currentUserId = authenticatedUserId(authentication, userId);
        cartService.addItemToCart(currentUserId, productId, quantity);
        return ResponseEntity.ok(cartService.getCartResponse(currentUserId));
    }

    @Operation(summary = "Update item quantity in cart")
    @PutMapping("/update")
    public ResponseEntity<CartResponse> updateItemQuantity(
            @RequestParam Integer userId,
            @RequestParam Integer productId,
            @RequestParam Integer quantity,
            Authentication authentication) {
        
        Integer currentUserId = authenticatedUserId(authentication, userId);
        cartService.updateItemQuantity(currentUserId, productId, quantity);
        return ResponseEntity.ok(cartService.getCartResponse(currentUserId));
    }

    @Operation(summary = "Remove an item from cart")
    @DeleteMapping("/remove")
    public ResponseEntity<CartResponse> removeItem(
            @RequestParam Integer userId,
            @RequestParam Integer productId,
            Authentication authentication) {
        
        Integer currentUserId = authenticatedUserId(authentication, userId);
        cartService.removeItemFromCart(currentUserId, productId);
        return ResponseEntity.ok(cartService.getCartResponse(currentUserId));
    }

    @Operation(summary = "Clear all items from cart")
    @DeleteMapping("/clear/{userId}")
    public ResponseEntity<CartResponse> clearCart(@PathVariable Integer userId, Authentication authentication) {
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
