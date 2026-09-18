package org.example.service;

import org.example.dto.AdminData;
import org.example.model.CardGame;
import org.example.model.MarketplaceStore;
import org.example.model.Order;
import org.example.model.OrderItem;
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

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class AdminDataService {
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final MarketplaceStoreRepository storeRepository;
    private final CardGameRepository cardGameRepository;
    private final UserRepository userRepository;

    public AdminDataService(ProductRepository productRepository,
                            OrderRepository orderRepository,
                            MarketplaceStoreRepository storeRepository,
                            CardGameRepository cardGameRepository,
                            UserRepository userRepository) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.storeRepository = storeRepository;
        this.cardGameRepository = cardGameRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public AdminData.DashboardResponse getDashboard() {
        List<Product> products = productRepository.findAllByOrderByProIdDesc();
        Map<Integer, Product> productsById = products.stream()
                .collect(Collectors.toMap(Product::getProId, product -> product, (first, ignored) -> first));
        Map<Integer, User> usersById = userRepository.findAll().stream()
                .collect(Collectors.toMap(User::getUaId, user -> user));
        List<Order> orders = orderRepository.findAllByOrderByCreatedAtDesc();

        double revenue = orders.stream().mapToDouble(order -> number(order.getTotalPrice())).sum();
        double expenses = orders.stream()
                .flatMap(order -> order.getItems().stream())
                .mapToDouble(item -> number(productsById.get(item.getProductId()) == null
                        ? null : productsById.get(item.getProductId()).getProCost()) * integer(item.getQuantity()))
                .sum();
        int totalItems = orders.stream().flatMap(order -> order.getItems().stream())
                .mapToInt(item -> integer(item.getQuantity())).sum();

        Map<YearMonth, double[]> metrics = new HashMap<>();
        for (Order order : orders) {
            if (order.getCreatedAt() == null) continue;
            YearMonth month = YearMonth.from(order.getCreatedAt());
            double[] value = metrics.computeIfAbsent(month, ignored -> new double[2]);
            value[0] += number(order.getTotalPrice());
            for (OrderItem item : order.getItems()) {
                Product product = productsById.get(item.getProductId());
                value[1] += number(product == null ? null : product.getProCost()) * integer(item.getQuantity());
            }
        }
        List<AdminData.MonthlyMetricResponse> monthlyMetrics = metrics.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new AdminData.MonthlyMetricResponse(
                        entry.getKey().format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH)),
                        entry.getValue()[0], entry.getValue()[1]))
                .toList();

        int activeOfficialProducts = (int) products.stream()
                .filter(this::isOfficial).filter(product -> Boolean.TRUE.equals(product.getIsActive())).count();
        int activeMarketplaceProducts = (int) products.stream()
                .filter(product -> "MARKETPLACE".equalsIgnoreCase(product.getListingSource()))
                .filter(product -> Boolean.TRUE.equals(product.getIsActive()))
                .filter(product -> "APPROVED".equalsIgnoreCase(product.getApprovalStatus())).count();
        int pendingStoreVerifications = (int) storeRepository.findAll().stream()
                .filter(store -> "PENDING".equalsIgnoreCase(store.getStoreStatus())).count();
        int pendingMarketplaceRequests = (int) products.stream()
                .filter(product -> "MARKETPLACE".equalsIgnoreCase(product.getListingSource()))
                .filter(product -> "PENDING".equalsIgnoreCase(product.getApprovalStatus())).count();
        int lowStockProducts = (int) products.stream().filter(this::isOfficial)
                .filter(product -> Boolean.TRUE.equals(product.getIsActive()))
                .filter(product -> integer(product.getProQuantity()) <= 5).count();

        return new AdminData.DashboardResponse(revenue, expenses, revenue - expenses, orders.size(), totalItems,
                orders.isEmpty() ? 0d : revenue / orders.size(), activeOfficialProducts, activeMarketplaceProducts,
                pendingStoreVerifications, pendingMarketplaceRequests, lowStockProducts, monthlyMetrics,
                orders.stream().limit(6).map(order -> toOrderResponse(order, productsById, usersById)).toList());
    }

    @Transactional(readOnly = true)
    public List<AdminData.ProductResponse> getOfficialProducts() {
        return productRepository.findAllByOrderByProIdDesc().stream()
                .filter(this::isOfficial).map(this::toProductResponse).toList();
    }

    @Transactional(readOnly = true)
    public AdminData.ProductResponse getOfficialProduct(Integer productId) {
        return productRepository.findByProId(productId).filter(this::isOfficial).map(this::toProductResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Official product not found"));
    }

    @Transactional
    public AdminData.ProductResponse createOfficialProduct(AdminData.ProductRequest request) {
        Product product = new Product();
        applyOfficialProduct(product, request);
        product.setListingSource("OFFICIAL");
        product.setApprovalStatus("APPROVED");
        product.setIsActive(true);
        product.setStore(null);
        product.setTemplateProductId(null);
        return toProductResponse(productRepository.saveAndFlush(product));
    }

    @Transactional
    public AdminData.ProductResponse updateOfficialProduct(Integer productId, AdminData.ProductRequest request) {
        Product product = productRepository.findByProId(productId).filter(this::isOfficial)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Official product not found"));
        applyOfficialProduct(product, request);
        return toProductResponse(productRepository.saveAndFlush(product));
    }

    @Transactional
    public void deactivateOfficialProduct(Integer productId) {
        Product product = productRepository.findByProId(productId).filter(this::isOfficial)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Official product not found"));
        product.setIsActive(false);
        productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public List<AdminData.CardGameResponse> getCardGames() {
        return cardGameRepository.findAllByOrderByGameNameAsc().stream()
                .map(game -> new AdminData.CardGameResponse(game.getGameId(), game.getGameName())).toList();
    }

    @Transactional(readOnly = true)
    public List<AdminData.OrderResponse> getOrders() {
        Map<Integer, Product> products = productMap();
        Map<Integer, User> users = userMap();
        return orderRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(order -> toOrderResponse(order, products, users)).toList();
    }

    @Transactional(readOnly = true)
    public AdminData.OrderResponse getOrder(Integer orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        order.getItems().size();
        return toOrderResponse(order, productMap(), userMap());
    }

    @Transactional
    public AdminData.OrderResponse updateOrderStatus(Integer orderId, String status) {
        String normalized = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        if (!List.of("PROCESSING", "SHIPPED", "DELIVERED", "CANCELED", "CANCELLED").contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid order status");
        }
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        order.setStatus(displayStatus(normalized));
        Order saved = orderRepository.saveAndFlush(order);
        saved.getItems().size();
        return toOrderResponse(saved, productMap(), userMap());
    }

    @Transactional(readOnly = true)
    public List<AdminData.StoreResponse> getStores(String status) {
        Map<Integer, Integer> productCounts = productCountsByStore();
        return storeRepository.findAll().stream()
                .filter(store -> status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)
                        || status.equalsIgnoreCase(store.getStoreStatus()))
                .sorted(Comparator.comparing(MarketplaceStore::getSubmittedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(store -> toStoreResponse(store, productCounts.getOrDefault(store.getStoreId(), 0))).toList();
    }

    @Transactional(readOnly = true)
    public AdminData.StoreResponse getStore(Integer storeId) {
        MarketplaceStore store = storeRepository.findByStoreId(storeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Store not found"));
        return toStoreResponse(store, productCountsByStore().getOrDefault(storeId, 0));
    }

    @Transactional(readOnly = true)
    public List<AdminData.ProductResponse> getMarketplaceProducts(Integer storeId) {
        return productRepository.findByListingSourceOrderByProIdDesc("MARKETPLACE").stream()
                .filter(product -> storeId == null || (product.getStore() != null
                        && Objects.equals(product.getStore().getStoreId(), storeId)))
                .map(this::toProductResponse).toList();
    }

    @Transactional(readOnly = true)
    public AdminData.ProductResponse getMarketplaceProduct(Integer productId) {
        return productRepository.findByProId(productId)
                .filter(product -> "MARKETPLACE".equalsIgnoreCase(product.getListingSource()))
                .map(this::toProductResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Marketplace product not found"));
    }

    private Map<Integer, Product> productMap() {
        return productRepository.findAllByOrderByProIdDesc().stream()
                .collect(Collectors.toMap(Product::getProId, product -> product, (first, ignored) -> first));
    }

    private Map<Integer, User> userMap() {
        return userRepository.findAll().stream()
                .collect(Collectors.toMap(User::getUaId, user -> user));
    }

    private Map<Integer, Integer> productCountsByStore() {
        return productRepository.findByListingSourceOrderByProIdDesc("MARKETPLACE").stream()
                .filter(product -> product.getStore() != null)
                .collect(Collectors.groupingBy(product -> product.getStore().getStoreId(), Collectors.summingInt(product -> 1)));
    }

    private void applyOfficialProduct(Product product, AdminData.ProductRequest request) {
        if (request == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product data is required");
        require(request.name(), "Product name is required");
        require(request.game(), "Card game is required");
        require(request.type(), "Product type is required");
        if (request.cost() == null || request.cost() < 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cost must be zero or more");
        if (request.price() == null || request.price() < 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Price must be zero or more");
        if (request.stock() == null || request.stock() < 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stock must be zero or more");
        CardGame game = cardGameRepository.findByGameNameIgnoreCase(request.game().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select a card game from the supported list"));
        product.setGameId(game.getGameId());
        product.setProName(request.name().trim());
        product.setProType(request.type().trim());
        product.setProCost(request.cost());
        product.setProPriceOfSell(request.price());
        product.setProQuantity(request.stock());
        product.setProImageUrl(blankToNull(request.imageUrl()));
        product.setProductSet(blankToNull(request.productSet()));
        product.setLanguage(blankToNull(request.language()));
        product.setProDescription(blankToNull(request.description()));
    }

    private AdminData.ProductResponse toProductResponse(Product product) {
        MarketplaceStore store = product.getStore();
        return new AdminData.ProductResponse(product.getProId(), product.getProName(),
                product.getCardGame() == null ? "Uncategorized" : product.getCardGame().getGameName(),
                product.getProType(), product.getProCost(), product.getProPriceOfSell(), product.getProQuantity(),
                product.getProImageUrl(), product.getProductSet(), product.getLanguage(), product.getProDescription(),
                product.getIsActive(), product.getListingSource(), product.getApprovalStatus(),
                store == null ? null : store.getStoreId(), store == null ? "Optracard Official Store" : store.getStoreName());
    }

    private AdminData.OrderResponse toOrderResponse(Order order, Map<Integer, Product> products, Map<Integer, User> users) {
        List<AdminData.OrderItemResponse> items = order.getItems().stream().map(item -> {
            Product product = products.get(item.getProductId());
            return new AdminData.OrderItemResponse(item.getProductId(),
                    product == null ? "Product unavailable" : product.getProName(),
                    product == null || product.getCardGame() == null ? "Unknown" : product.getCardGame().getGameName(),
                    product == null ? null : product.getProImageUrl(), item.getPrice(), item.getQuantity());
        }).toList();
        User user = users.get(order.getUserId());
        return new AdminData.OrderResponse(order.getOrderId(), orderNumber(order), order.getUserId(),
                user == null ? "Unknown customer" : user.getUsername(), order.getCreatedAt(), order.getTotalPrice(),
                order.getSource(), order.getStoreId(), order.getStoreName(), displayStatus(order.getStatus()),
                order.getPaymentStatus(), order.getShippingMethod(), order.getShippingAddress(), order.getRecipientName(),
                order.getRecipientPhone(), order.getTrackingNumber(), items);
    }

    private AdminData.StoreResponse toStoreResponse(MarketplaceStore store, int productCount) {
        String ownerName = java.util.stream.Stream.of(store.getOwnerFirstName(), store.getOwnerLastName())
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.joining(" "));
        return new AdminData.StoreResponse(store.getStoreId(), store.getStoreSlug(), store.getStoreStatus(),
                store.getStoreName(), store.getStoreDescription(), store.getPhysicalStore(), ownerName,
                store.getOwnerEmail(), store.getOwnerPhone(), store.getBankName(), store.getBankBranch(),
                store.getBankAccountName(), store.getBankAccountNumber(), store.getStoreAddress(), store.getProvince(),
                store.getDistrict(), store.getSubdistrict(), store.getPostalCode(), store.getStoreProfileImage(),
                store.getBankPassbookImage(), store.getTermsAccepted(), store.getSubmittedAt(), store.getReviewedAt(),
                store.getReviewNote(), productCount);
    }

    private boolean isOfficial(Product product) {
        return !"MARKETPLACE".equalsIgnoreCase(product.getListingSource());
    }

    private String orderNumber(Order order) {
        String date = order.getCreatedAt() == null ? "UNKNOWN" : order.getCreatedAt().toLocalDate()
                .format(DateTimeFormatter.BASIC_ISO_DATE);
        return "ORD-" + date + "-" + String.format("%04d", order.getOrderId());
    }

    private String displayStatus(String status) {
        if (status == null) return "Processing";
        return switch (status.trim().toUpperCase(Locale.ROOT)) {
            case "SHIPPED" -> "Shipped";
            case "DELIVERED", "COMPLETED" -> "Delivered";
            case "CANCELED", "CANCELLED" -> "Canceled";
            default -> "Processing";
        };
    }

    private double number(Double value) { return value == null ? 0d : value; }
    private int integer(Integer value) { return value == null ? 0 : value; }
    private void require(String value, String message) {
        if (value == null || value.trim().isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
    private String blankToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
