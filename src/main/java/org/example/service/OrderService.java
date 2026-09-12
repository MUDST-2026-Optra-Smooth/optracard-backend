package org.example.service;

import org.example.dto.CheckoutResponse;
import org.example.dto.CreateOrderRequest;
import org.example.dto.OrderItemResponse;
import org.example.dto.OrderResponse;
import org.example.model.Cart;
import org.example.model.CartItem;
import org.example.model.Order;
import org.example.model.OrderItem;
import org.example.model.Product;
import org.example.repository.CartRepository;
import org.example.repository.OrderRepository;
import org.example.repository.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderService {
    private static final double STANDARD_DELIVERY_FEE = 50d;
    private static final String OFFICIAL_STORE_NAME = "Optracard Official Store";

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;

    public OrderService(OrderRepository orderRepository, CartRepository cartRepository, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
    }

    /**
     * A cart may contain products sold by several sellers. Each seller receives
     * a separate order so payment, shipping, and order history stay unambiguous.
     */
    @Transactional
    public CheckoutResponse placeOrder(Integer userId, CreateOrderRequest request) {
        if (request == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order data is required");
        String shipping = normalize(request.shippingMethod());
        String payment = normalize(request.paymentMethod());
        validateRequest(request, shipping, payment);

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Your cart is empty"));
        if (cart.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Your cart is empty");
        }

        // Validate every product before changing stock or creating any order.
        Map<Seller, List<CartLine>> itemsBySeller = new LinkedHashMap<>();
        for (CartItem cartItem : cart.getItems()) {
            Product product = productRepository.findById(cartItem.getProductId())
                    .filter(this::isPurchasable)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "A product in your cart is no longer available"));
            int quantity = cartItem.getQuantity();
            if (quantity < 1 || product.getProQuantity() == null || product.getProQuantity() < quantity) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not enough stock for " + product.getProName());
            }
            Seller seller = sellerFor(product);
            itemsBySeller.computeIfAbsent(seller, ignored -> new ArrayList<>())
                    .add(new CartLine(product, quantity));
        }

        if ("pickup".equalsIgnoreCase(shipping) && itemsBySeller.size() > 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Store pickup is available only when every item is from the same seller. Choose Standard Delivery.");
        }

        List<OrderResponse> placedOrders = new ArrayList<>();
        for (Map.Entry<Seller, List<CartLine>> entry : itemsBySeller.entrySet()) {
            Seller seller = entry.getKey();
            Order order = newOrder(userId, request, payment, shipping, seller);
            double itemsTotal = 0d;

            for (CartLine line : entry.getValue()) {
                Product product = line.product();
                double price = product.getProPriceOfSell() == null ? 0d : product.getProPriceOfSell();
                itemsTotal += price * line.quantity();

                OrderItem item = new OrderItem();
                item.setOrder(order);
                item.setProductId(product.getProId());
                item.setQuantity(line.quantity());
                item.setPrice(price);
                order.getItems().add(item);
                product.setProQuantity(product.getProQuantity() - line.quantity());
            }
            order.setTotalPrice(itemsTotal + order.getShippingFee());
            placedOrders.add(toResponse(orderRepository.saveAndFlush(order)));
        }

        cart.getItems().clear();
        cartRepository.save(cart);
        double total = placedOrders.stream().mapToDouble(order -> order.total() == null ? 0d : order.total()).sum();
        return new CheckoutResponse(placedOrders, placedOrders.size(), total);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrderHistory(Integer userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::toResponse).toList();
    }

    private Order newOrder(Integer userId, CreateOrderRequest request, String payment, String shipping, Seller seller) {
        Order order = new Order();
        order.setUserId(userId);
        order.setStoreId(seller.storeId());
        order.setStoreName(seller.storeName());
        order.setSource(seller.source());
        order.setPaymentMethod(payment);
        order.setPaymentStatus("PENDING");
        order.setStatus("Processing");
        order.setShippingMethod(shipping);
        order.setShippingFee("standard".equalsIgnoreCase(shipping) ? STANDARD_DELIVERY_FEE : 0d);
        order.setRecipientName(normalize(request.recipientName()));
        order.setRecipientPhone(normalize(request.recipientPhone()));
        order.setShippingAddress(normalize(request.shippingAddress()));
        return order;
    }

    private Seller sellerFor(Product product) {
        boolean marketplace = "MARKETPLACE".equalsIgnoreCase(product.getListingSource());
        if (!marketplace) return new Seller(null, OFFICIAL_STORE_NAME, "OFFICIAL");
        return new Seller(product.getStore().getStoreId(), product.getStore().getStoreName(), "MARKETPLACE");
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream().map(item -> {
            Product product = productRepository.findById(item.getProductId()).orElse(null);
            String name = product == null ? "Product unavailable" : product.getProName();
            String game = product == null || product.getCardGame() == null ? "Unknown" : product.getCardGame().getGameName();
            String image = product == null ? null : product.getProImageUrl();
            String store = product == null || product.getStore() == null
                    ? OFFICIAL_STORE_NAME : product.getStore().getStoreName();
            return new OrderItemResponse(item.getProductId(), name, game, image, item.getPrice(), item.getQuantity(), store);
        }).toList();

        String storeName = normalize(order.getStoreName());
        if (storeName == null) storeName = items.isEmpty() ? OFFICIAL_STORE_NAME : items.get(0).storeName();
        String source = normalize(order.getSource());
        if (source == null) source = "OFFICIAL";
        return new OrderResponse(order.getOrderId(), orderNumber(order), order.getStoreId(), storeName, source,
                order.getCreatedAt(), order.getTotalPrice(), order.getPaymentMethod(), order.getPaymentStatus(),
                displayStatus(order.getStatus()), order.getShippingMethod(), order.getShippingFee(),
                order.getShippingAddress(), order.getRecipientName(), order.getRecipientPhone(), items);
    }

    private void validateRequest(CreateOrderRequest request, String shipping, String payment) {
        if (!"pickup".equalsIgnoreCase(shipping) && !"standard".equalsIgnoreCase(shipping)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid shipping method");
        }
        if (!List.of("cash", "credit-card", "qr").contains(payment)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid payment method");
        }
        if (!"pickup".equalsIgnoreCase(shipping)) {
            require(request.recipientName(), "Recipient name is required");
            require(request.recipientPhone(), "Recipient phone is required");
            require(request.shippingAddress(), "Shipping address is required");
        }
    }

    private String orderNumber(Order order) {
        String date = order.getCreatedAt() == null ? LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                : order.getCreatedAt().toLocalDate().format(DateTimeFormatter.BASIC_ISO_DATE);
        return "ORD-" + date + "-" + String.format("%04d", order.getOrderId() == null ? 0 : order.getOrderId());
    }

    private String displayStatus(String status) {
        if (status == null) return "Processing";
        return switch (status.trim().toUpperCase()) {
            case "SHIPPED" -> "Shipped";
            case "DELIVERED", "COMPLETED" -> "Delivered";
            case "CANCELED", "CANCELLED" -> "Canceled";
            default -> "Processing";
        };
    }

    private String normalize(String value) {
        if (value == null) return null;
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private void require(String value, String message) {
        if (normalize(value) == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private boolean isPurchasable(Product product) {
        return Boolean.TRUE.equals(product.getIsActive())
                && (!"MARKETPLACE".equalsIgnoreCase(product.getListingSource())
                || (product.getStore() != null && "APPROVED".equalsIgnoreCase(product.getStore().getStoreStatus())));
    }

    private record Seller(Integer storeId, String storeName, String source) {}
    private record CartLine(Product product, int quantity) {}
}
