package org.example.controller;

import org.example.model.Cart;
import org.example.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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

    // 1. Endpoint สำหรับดูตะกร้าของ User 
    // ตัวอย่างการเรียกใช้งาน: GET http://localhost:8080/api/cart/1
    @GetMapping("/{userId}")
    public ResponseEntity<Cart> getCart(@PathVariable Integer userId) {
        Cart cart = cartService.getCartByUserId(userId);
        return ResponseEntity.ok(cart);
    }

    // 2. Endpoint สำหรับเพิ่มสินค้าลงตะกร้า
    // ตัวอย่างการเรียกใช้งาน: POST http://localhost:8080/api/cart/add?userId=1&productId=101&quantity=2
    @PostMapping("/add")
    public ResponseEntity<Cart> addItemToCart(
            @RequestParam Integer userId,
            @RequestParam Integer productId,
            @RequestParam Integer quantity) {
        
        Cart updatedCart = cartService.addItemToCart(userId, productId, quantity);
        return ResponseEntity.ok(updatedCart);
    }

    // 3. Endpoint สำหรับอัปเดตจำนวนสินค้า (ใช้ PUT)
    // ตัวอย่าง: PUT http://localhost:8080/api/cart/update?userId=1&productId=1&quantity=5
    @PutMapping("/update")
    public ResponseEntity<Cart> updateItemQuantity(
            @RequestParam Integer userId,
            @RequestParam Integer productId,
            @RequestParam Integer quantity) {
        
        Cart updatedCart = cartService.updateItemQuantity(userId, productId, quantity);
        return ResponseEntity.ok(updatedCart);
    }

    // 4. Endpoint สำหรับลบสินค้าออกจากตะกร้า (ใช้ DELETE)
    // ตัวอย่าง: DELETE http://localhost:8080/api/cart/remove?userId=1&productId=1
    @DeleteMapping("/remove")
    public ResponseEntity<Cart> removeItem(
            @RequestParam Integer userId,
            @RequestParam Integer productId) {
        
        Cart updatedCart = cartService.removeItemFromCart(userId, productId);
        return ResponseEntity.ok(updatedCart);
    }

    // 5. Endpoint สำหรับล้างตะกร้าทั้งหมด (ใช้ DELETE)
    // ตัวอย่าง: DELETE http://localhost:8080/api/cart/clear/1
    @DeleteMapping("/clear/{userId}")
    public ResponseEntity<Cart> clearCart(@PathVariable Integer userId) {
        
        Cart updatedCart = cartService.clearCart(userId);
        return ResponseEntity.ok(updatedCart);
    }
}