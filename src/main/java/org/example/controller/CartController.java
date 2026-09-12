package org.example.controller;

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
@CrossOrigin(origins = "*") // สำคัญมาก! อนุญาตให้ React ที่รันอยู่คนละพอร์ตสามารถเรียกใช้ API นี้ได้โดยไม่ติด Error CORS
public class CartController {

    @Autowired
    private CartService cartService;

    @Autowired
    private UserRepository userRepository;

    // 1. Endpoint สำหรับดูตะกร้าของ User 
    // ตัวอย่างการเรียกใช้งาน: GET http://localhost:8080/api/cart/1
    @GetMapping("/{userId}")
    public ResponseEntity<CartResponse> getCart(@PathVariable Integer userId, Authentication authentication) {
        Integer currentUserId = authenticatedUserId(authentication, userId);
        return ResponseEntity.ok(cartService.getCartResponse(currentUserId));
    }

    // 2. Endpoint สำหรับเพิ่มสินค้าลงตะกร้า
    // ตัวอย่างการเรียกใช้งาน: POST http://localhost:8080/api/cart/add?userId=1&productId=101&quantity=2
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

    // 3. Endpoint สำหรับอัปเดตจำนวนสินค้า (ใช้ PUT)
    // ตัวอย่าง: PUT http://localhost:8080/api/cart/update?userId=1&productId=1&quantity=5
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

    // 4. Endpoint สำหรับลบสินค้าออกจากตะกร้า (ใช้ DELETE)
    // ตัวอย่าง: DELETE http://localhost:8080/api/cart/remove?userId=1&productId=1
    @DeleteMapping("/remove")
    public ResponseEntity<CartResponse> removeItem(
            @RequestParam Integer userId,
            @RequestParam Integer productId,
            Authentication authentication) {
        
        Integer currentUserId = authenticatedUserId(authentication, userId);
        cartService.removeItemFromCart(currentUserId, productId);
        return ResponseEntity.ok(cartService.getCartResponse(currentUserId));
    }

    // 5. Endpoint สำหรับล้างตะกร้าทั้งหมด (ใช้ DELETE)
    // ตัวอย่าง: DELETE http://localhost:8080/api/cart/clear/1
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
