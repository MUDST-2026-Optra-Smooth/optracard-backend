package org.example.service;

import org.example.dto.ReorderResponse;
import org.example.model.Cart;
import org.example.model.CartItem;
import org.example.model.Order;
import org.example.model.OrderItem;
import org.example.model.Product;
import org.example.repository.OrderRepository;
import org.example.repository.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Puts the items of a past order back into the buyer's cart.
 * <p>
 * Every item is checked before it is added, so the cart is never changed by a
 * request that fails halfway. Items that cannot be added are reported back with
 * a reason instead of being dropped silently.
 */
@Service
public class ReorderService {
    private static final String UNKNOWN_PRODUCT_NAME = "Product unavailable";

    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CartService cartService;

    public ReorderService(OrderService orderService, OrderRepository orderRepository,
                          ProductRepository productRepository, CartService cartService) {
        this.orderService = orderService;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.cartService = cartService;
    }

    @Transactional
    public ReorderResponse reorder(Integer userId, String requestedOrderNumber) {
        String orderNumber = requestedOrderNumber == null ? "" : requestedOrderNumber.trim();
        if (orderNumber.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order number is required");
        }

        // Only the buyer's own orders are searched, so another user's order number looks like "not found".
        Order order = orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(candidate -> orderNumber.equals(orderService.orderNumber(candidate)))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        Map<Integer, Integer> requested = new LinkedHashMap<>();
        for (OrderItem item : order.getItems()) {
            requested.merge(item.getProductId(), item.getQuantity() == null ? 0 : item.getQuantity(), Integer::sum);
        }

        Cart cart = cartService.getCartByUserId(userId);
        List<ReorderResponse.Line> added = new ArrayList<>();
        List<ReorderResponse.Line> skipped = new ArrayList<>();

        requested.forEach((productId, quantity) -> {
            Product product = productRepository.findById(productId).orElse(null);
            if (product == null || !cartService.isPurchasable(product)) {
                String name = product == null || isBlank(product.getProName()) ? UNKNOWN_PRODUCT_NAME : product.getProName();
                skipped.add(new ReorderResponse.Line(productId, name, quantity, 0, ReorderResponse.UNAVAILABLE,
                        name + " is no longer available."));
                return;
            }

            int stock = product.getProQuantity() == null ? 0 : product.getProQuantity();
            int inCart = cart.getItems().stream()
                    .filter(item -> productId.equals(item.getProductId()))
                    .mapToInt(CartItem::getQuantity)
                    .sum();
            int room = Math.max(0, stock - inCart);
            int toAdd = Math.min(quantity, room);

            if (toAdd < 1) {
                String message = stock < 1
                        ? product.getProName() + " is out of stock."
                        : "Your cart already holds all " + stock + " available of " + product.getProName() + ".";
                skipped.add(new ReorderResponse.Line(productId, product.getProName(), quantity, 0,
                        ReorderResponse.OUT_OF_STOCK, message));
                return;
            }

            cartService.addItemToCart(userId, productId, toAdd);
            if (toAdd < quantity) {
                added.add(new ReorderResponse.Line(productId, product.getProName(), quantity, toAdd,
                        ReorderResponse.LIMITED_STOCK,
                        "Only " + toAdd + " of " + quantity + " " + product.getProName() + " could be added because of limited stock."));
            } else {
                added.add(new ReorderResponse.Line(productId, product.getProName(), quantity, toAdd, null, null));
            }
        });

        return new ReorderResponse(orderNumber, added.size(), skipped.size(), added, skipped);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
