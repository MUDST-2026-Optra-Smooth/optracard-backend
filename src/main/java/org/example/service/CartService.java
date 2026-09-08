package org.example.service;

import org.example.model.Cart;
import org.example.model.CartItem;
import org.example.repository.CartRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class CartService {

    @Autowired
    private CartRepository cartRepository;

    // 1. ฟังก์ชันดึงข้อมูลตะกร้าของ User (ถ้าไม่มีระบบจะสร้างให้ใหม่ทันที)
    public Cart getCartByUserId(Integer userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUserId(userId);
                    return cartRepository.save(newCart);
                });
    }

    // 2. ฟังก์ชันเพิ่มสินค้าลงตะกร้า
    @Transactional
    public Cart addItemToCart(Integer userId, Integer productId, Integer quantity) {
        // ไปเอาตะกร้าของ User คนนี้มา
        Cart cart = getCartByUserId(userId);

        // ค้นหาดูว่าในตะกร้ามีสินค้านี้อยู่ก่อนแล้วหรือเปล่า
        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst();

        if (existingItem.isPresent()) {
            // กรณีที่ 1: มีสินค้านี้อยู่แล้ว -> จับจำนวนมาบวกเพิ่มเข้าไป
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + quantity);
        } else {
            // กรณีที่ 2: ยังไม่เคยมีสินค้านี้ในตะกร้า -> สร้างรายการใหม่
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setProductId(productId);
            newItem.setQuantity(quantity);
            cart.getItems().add(newItem); // จับใส่ตะกร้า
        }

        // สั่งบันทึกการเปลี่ยนแปลงทั้งหมดลง Database
        return cartRepository.save(cart);
    }

    // 3. ฟังก์ชันปรับเปลี่ยนจำนวนสินค้า (เพิ่ม/ลด)
    @Transactional
    public Cart updateItemQuantity(Integer userId, Integer productId, Integer quantity) {
        Cart cart = getCartByUserId(userId);
        
        cart.getItems().stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst()
                .ifPresent(item -> {
                    if (quantity > 0) {
                        item.setQuantity(quantity); // อัปเดตจำนวนใหม่
                    } else {
                        cart.getItems().remove(item); // ถ้าจำนวนเป็น 0 หรือติดลบ ให้ลบออกจากตะกร้าเลย
                    }
                });
                
        return cartRepository.save(cart);
    }

    // 4. ฟังก์ชันลบสินค้า 1 ชนิดออกจากตะกร้า
    @Transactional
    public Cart removeItemFromCart(Integer userId, Integer productId) {
        Cart cart = getCartByUserId(userId);
        // ลบ item ที่มี productId ตรงกับที่ระบุ
        cart.getItems().removeIf(item -> item.getProductId().equals(productId));
        return cartRepository.save(cart);
    }

    // 5. ฟังก์ชันล้างตะกร้าทั้งหมด (Clear Cart)
    @Transactional
    public Cart clearCart(Integer userId) {
        Cart cart = getCartByUserId(userId);
        cart.getItems().clear(); // ล้างของทุกชิ้นในตะกร้า
        return cartRepository.save(cart);
    }
}