package org.example.service;

import org.example.dto.CartItemResponse;
import org.example.dto.CartResponse;
import org.example.model.Cart;
import org.example.model.CartItem;
import org.example.model.Product;
import org.example.repository.CartRepository;
import org.example.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class CartService {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private ProductRepository productRepository;

    // 1. ฟังก์ชันดึงข้อมูลตะกร้าของ User (ถ้าไม่มีระบบจะสร้างให้ใหม่ทันที)
    @Transactional
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
        if (quantity == null || quantity < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be at least 1");
        }
        Product product = productRepository.findById(productId)
                .filter(this::isPurchasable)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        if (product.getProQuantity() == null || product.getProQuantity() < quantity) {
            throw insufficientStock(product);
        }
        // ไปเอาตะกร้าของ User คนนี้มา
        Cart cart = getCartByUserId(userId);

        // ค้นหาดูว่าในตะกร้ามีสินค้านี้อยู่ก่อนแล้วหรือเปล่า
        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst();

        if (existingItem.isPresent()) {
            // กรณีที่ 1: มีสินค้านี้อยู่แล้ว -> จับจำนวนมาบวกเพิ่มเข้าไป
            CartItem item = existingItem.get();
            int updatedQuantity = item.getQuantity() + quantity;
            if (updatedQuantity > product.getProQuantity()) {
                throw insufficientStock(product);
            }
            item.setQuantity(updatedQuantity);
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
        if (quantity == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity is required");
        }
        if (quantity > 0) {
            Product product = productRepository.findById(productId)
                    .filter(this::isPurchasable)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
            if (product.getProQuantity() == null || quantity > product.getProQuantity()) {
                throw insufficientStock(product);
            }
        }
        
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

    @Transactional
    public CartResponse getCartResponse(Integer userId) {
        Cart cart = getCartByUserId(userId);
        List<CartItemResponse> items = cart.getItems().stream().map(item -> {
            Product product = productRepository.findById(item.getProductId()).orElse(null);
            if (product == null) return null;
            String game = product.getCardGame() == null ? "Unknown" : product.getCardGame().getGameName();
            Integer storeId = product.getStore() == null ? null : product.getStore().getStoreId();
            String store = product.getStore() == null ? "Optracard Official Store" : product.getStore().getStoreName();
            return new CartItemResponse(product.getProId(), item.getQuantity(), product.getProName(), game,
                    product.getProType(), product.getProPriceOfSell(), product.getProQuantity(), product.getProImageUrl(),
                    storeId, store, product.getListingSource());
        }).filter(java.util.Objects::nonNull).toList();
        double subtotal = items.stream().mapToDouble(item -> (item.price() == null ? 0d : item.price()) * item.quantity()).sum();
        int count = items.stream().mapToInt(CartItemResponse::quantity).sum();
        return new CartResponse(cart.getCartId(), items, subtotal, count);
    }

    private boolean isPurchasable(Product product) {
        return Boolean.TRUE.equals(product.getIsActive())
                && (
                !"MARKETPLACE".equalsIgnoreCase(product.getListingSource())
                        || (product.getStore() != null
                        && "APPROVED".equalsIgnoreCase(product.getStore().getStoreStatus()))
        );
    }

    private ResponseStatusException insufficientStock(Product product) {
        int availableStock = product.getProQuantity() == null ? 0 : product.getProQuantity();
        String productName = product.getProName() == null || product.getProName().isBlank()
                ? "this product"
                : product.getProName();
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Only " + availableStock + " item" + (availableStock == 1 ? "" : "s")
                        + " of " + productName + " are available in stock."
        );
    }
}
