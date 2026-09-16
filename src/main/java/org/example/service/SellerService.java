package org.example.service;

import org.example.dto.OrderItemResponse;
import org.example.dto.CardGameOptionResponse;
import org.example.dto.SellerOrderResponse;
import org.example.dto.SellerProductRequest;
import org.example.dto.SellerProductResponse;
import org.example.model.CardGame;
import org.example.model.MarketplaceStore;
import org.example.model.Order;
import org.example.model.Product;
import org.example.model.User;
import org.example.repository.CardGameRepository;
import org.example.repository.MarketplaceStoreRepository;
import org.example.repository.OrderRepository;
import org.example.repository.ProductRepository;
import org.example.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class SellerService {
    private final ProductRepository productRepository;
    private final MarketplaceStoreRepository storeRepository;
    private final UserRepository userRepository;
    private final CardGameRepository cardGameRepository;
    private final OrderRepository orderRepository;

    public SellerService(ProductRepository productRepository,
                         MarketplaceStoreRepository storeRepository,
                         UserRepository userRepository,
                         CardGameRepository cardGameRepository,
                         OrderRepository orderRepository) {
        this.productRepository = productRepository;
        this.storeRepository = storeRepository;
        this.userRepository = userRepository;
        this.cardGameRepository = cardGameRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public List<SellerProductResponse> getProducts(String email) {
        MarketplaceStore store = requireApprovedStore(email);
        return productRepository.findByStore_StoreIdOrderByProIdDesc(store.getStoreId())
                .stream().map(this::toProductResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<CardGameOptionResponse> getCardGames() {
        return cardGameRepository.findAllByOrderByGameNameAsc()
                .stream()
                .map(game -> new CardGameOptionResponse(game.getGameId(), game.getGameName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public SellerProductResponse getProduct(String email, Integer productId) {
        MarketplaceStore store = requireApprovedStore(email);
        return productRepository.findByProIdAndStore_StoreId(productId, store.getStoreId())
                .map(this::toProductResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }

    @Transactional
    public SellerProductResponse createProduct(String email, SellerProductRequest request) {
        MarketplaceStore store = requireApprovedStore(email);
        validate(request);
        Product product = new Product();
        apply(product, request, store, request.templateProductId());
        product.setListingSource("MARKETPLACE");
        product.setApprovalStatus("PENDING");
        product.setIsActive(true);
        product.setStore(store);
        return toProductResponse(productRepository.saveAndFlush(product));
    }

    @Transactional
    public SellerProductResponse updateProduct(String email, Integer productId, SellerProductRequest request) {
        MarketplaceStore store = requireApprovedStore(email);
        validate(request);
        Product product = productRepository.findByProIdAndStore_StoreId(productId, store.getStoreId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        Integer templateId = request.templateProductId() == null
                ? product.getTemplateProductId()
                : request.templateProductId();
        apply(product, request, store, templateId);
        product.setListingSource("MARKETPLACE");
        product.setApprovalStatus("PENDING");
        product.setIsActive(true);
        return toProductResponse(productRepository.saveAndFlush(product));
    }

    @Transactional
    public void deactivateProduct(String email, Integer productId) {
        MarketplaceStore store = requireApprovedStore(email);
        Product product = productRepository.findByProIdAndStore_StoreId(productId, store.getStoreId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        product.setIsActive(false);
        productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public List<SellerOrderResponse> getOrders(String email) {
        MarketplaceStore store = requireApprovedStore(email);
        return orderRepository.findByStoreIdOrderByCreatedAtDesc(store.getStoreId())
                .stream().map(this::toOrderResponse).toList();
    }

    @Transactional
    public SellerOrderResponse updateOrderStatus(String email, Integer orderId, String status) {
        MarketplaceStore store = requireApprovedStore(email);
        String normalized = status == null ? "" : status.trim().toUpperCase();
        if (!List.of("PROCESSING", "SHIPPED", "DELIVERED", "CANCELED").contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid order status");
        }
        Order order = orderRepository.findByOrderIdAndStoreId(orderId, store.getStoreId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        order.setStatus(normalized.substring(0, 1) + normalized.substring(1).toLowerCase());
        return toOrderResponse(orderRepository.saveAndFlush(order));
    }

    @Transactional(readOnly = true)
    public MarketplaceStore requireApprovedStore(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        MarketplaceStore store = storeRepository.findFirstBySellerUserIdOrderByStoreIdDesc(user.getUaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "An approved seller store is required"));
        if (!"APPROVED".equalsIgnoreCase(store.getStoreStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Your seller store is not approved yet");
        }
        return store;
    }

    private void apply(Product product, SellerProductRequest request, MarketplaceStore store, Integer templateProductId) {
        Product template = templateProductId == null ? null : productRepository.findByProId(templateProductId)
                .filter(this::isApprovedMarketplaceTemplate)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "The selected Marketplace product is no longer available as a template"
                ));
        if (template != null && template.getStore() != null
                && template.getStore().getStoreId().equals(store.getStoreId())
                && !template.getProId().equals(product.getProId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This product is already listed in your shop");
        }

        CardGame game;
        String productName;
        String productType;
        if (template != null) {
            game = template.getCardGame();
            if (game == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The selected product has no valid card game");
            }
            productName = template.getProName();
            productType = template.getProType();
        } else {
            game = cardGameRepository.findByGameNameIgnoreCase(request.game().trim())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Please select a card game from the supported list"
                    ));
            productName = request.name().trim();
            productType = request.type().trim();
        }
        if (productRepository.existsByProNameIgnoreCaseAndProTypeAndGameIdAndListingSource(
                productName, productType, game.getGameId(), "OFFICIAL")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "This product is already sold by Optracard Official Store and cannot be listed on Marketplace"
            );
        }
        product.setGameId(game.getGameId());
        product.setProName(productName);
        product.setProType(productType);
        product.setProCost(request.cost());
        product.setProPriceOfSell(request.price());
        product.setProQuantity(request.stock());
        product.setProImageUrl(template == null ? blankToNull(request.imageUrl()) : template.getProImageUrl());
        product.setProductSet(template == null ? blankToNull(request.productSet()) : template.getProductSet());
        product.setLanguage(template == null ? blankToNull(request.language()) : template.getLanguage());
        product.setProDescription(blankToNull(request.description()));
        product.setTemplateProductId(template == null ? null : template.getProId());
    }

    private boolean isApprovedMarketplaceTemplate(Product product) {
        return "MARKETPLACE".equalsIgnoreCase(product.getListingSource())
                && Boolean.TRUE.equals(product.getIsActive())
                && "APPROVED".equalsIgnoreCase(product.getApprovalStatus())
                && product.getStore() != null
                && "APPROVED".equalsIgnoreCase(product.getStore().getStoreStatus());
    }

    private void validate(SellerProductRequest request) {
        if (request == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product data is required");
        require(request.name(), "Product name is required");
        require(request.game(), "Card game is required");
        require(request.type(), "Product type is required");
        if (request.cost() == null || request.cost() < 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cost must be zero or more");
        if (request.price() == null || request.price() < 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Price must be zero or more");
        if (request.stock() == null || request.stock() < 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stock must be zero or more");
    }

    private void require(String value, String message) {
        if (value == null || value.trim().isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private String blankToNull(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        return value.trim();
    }

    private SellerProductResponse toProductResponse(Product product) {
        return new SellerProductResponse(product.getProId(), product.getProName(),
                product.getCardGame() == null ? "Uncategorized" : product.getCardGame().getGameName(),
                product.getProType(), product.getProCost(), product.getProPriceOfSell(), product.getProQuantity(),
                product.getProImageUrl(), product.getProductSet(), product.getLanguage(), product.getProDescription(),
                product.getApprovalStatus(), product.getIsActive(), product.getStore().getStoreId(), product.getStore().getStoreName());
    }

    private SellerOrderResponse toOrderResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream().map(item -> {
            Product product = productRepository.findById(item.getProductId()).orElse(null);
            return new OrderItemResponse(item.getProductId(), product == null ? "Product unavailable" : product.getProName(),
                    product == null || product.getCardGame() == null ? "Unknown" : product.getCardGame().getGameName(),
                    product == null ? null : product.getProImageUrl(), item.getPrice(), item.getQuantity(), order.getStoreName());
        }).toList();
        return new SellerOrderResponse(order.getOrderId(), orderNumber(order), order.getUserId(), order.getCreatedAt(),
                order.getTotalPrice(), displayStatus(order.getStatus()), order.getPaymentStatus(), order.getShippingMethod(),
                order.getShippingAddress(), order.getRecipientName(), order.getRecipientPhone(), items);
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
}
