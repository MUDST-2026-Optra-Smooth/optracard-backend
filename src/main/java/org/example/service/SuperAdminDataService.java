package org.example.service;

import org.example.dto.SuperAdminData;
import org.example.model.MarketplaceStore;
import org.example.model.Order;
import org.example.model.Product;
import org.example.model.User;
import org.example.repository.MarketplaceStoreRepository;
import org.example.repository.CartRepository;
import org.example.repository.OrderRepository;
import org.example.repository.ProductRepository;
import org.example.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/** Provides the operational, database-backed data shown to super administrators. */
@Service
public class SuperAdminDataService {
    private final MarketplaceStoreRepository storeRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CartRepository cartRepository;

    public SuperAdminDataService(MarketplaceStoreRepository storeRepository,
                                 ProductRepository productRepository,
                                 OrderRepository orderRepository,
                                 UserRepository userRepository,
                                 PasswordEncoder passwordEncoder,
                                 CartRepository cartRepository) {
        this.storeRepository = storeRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.cartRepository = cartRepository;
    }

    @Transactional(readOnly = true)
    public SuperAdminData.OverviewResponse getOverview() {
        List<MarketplaceStore> stores = storeRepository.findAll();
        List<Product> products = productRepository.findAllByOrderByProIdDesc();
        List<Order> orders = orderRepository.findAllByOrderByCreatedAtDesc();
        Map<Integer, User> usersById = userMap();

        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<Order> lastThirtyDays = orders.stream()
                .filter(order -> order.getCreatedAt() != null && !order.getCreatedAt().isBefore(thirtyDaysAgo))
                .toList();
        double gmvLast30Days = lastThirtyDays.stream().mapToDouble(order -> number(order.getTotalPrice())).sum();

        Map<Integer, List<Product>> productsByStore = products.stream()
                .filter(this::isMarketplace)
                .filter(product -> product.getStore() != null)
                .collect(Collectors.groupingBy(product -> product.getStore().getStoreId()));
        Map<Integer, List<Order>> ordersByStore = orders.stream()
                .filter(order -> order.getStoreId() != null)
                .collect(Collectors.groupingBy(Order::getStoreId));

        List<SuperAdminData.StorePerformanceResponse> topStores = stores.stream()
                .map(store -> toStorePerformance(store, productsByStore.getOrDefault(store.getStoreId(), List.of()),
                        ordersByStore.getOrDefault(store.getStoreId(), List.of()), usersById))
                .sorted(Comparator.comparingDouble(SuperAdminData.StorePerformanceResponse::gmv).reversed()
                        .thenComparing(SuperAdminData.StorePerformanceResponse::storeName,
                                Comparator.nullsLast(String::compareToIgnoreCase)))
                .limit(8)
                .toList();

        int inStock = (int) products.stream().filter(this::isVisibleListing)
                .filter(product -> integer(product.getProQuantity()) > 5).count();
        int lowStock = (int) products.stream().filter(this::isVisibleListing)
                .filter(product -> integer(product.getProQuantity()) > 0 && integer(product.getProQuantity()) <= 5).count();
        int outOfStock = (int) products.stream().filter(this::isVisibleListing)
                .filter(product -> integer(product.getProQuantity()) == 0).count();

        return new SuperAdminData.OverviewResponse(
                stores.size(),
                (int) stores.stream().filter(store -> "APPROVED".equalsIgnoreCase(store.getStoreStatus())).count(),
                (int) stores.stream().filter(store -> "PENDING".equalsIgnoreCase(store.getStoreStatus())).count(),
                products.size(), (int) products.stream().filter(this::isVisibleListing).count(),
                gmvLast30Days, lastThirtyDays.size(),
                lastThirtyDays.isEmpty() ? 0d : gmvLast30Days / lastThirtyDays.size(),
                inStock, lowStock, outOfStock,
                monthlyMetrics(orders), topStores, catalogByGame(products)
        );
    }

    @Transactional(readOnly = true)
    public List<SuperAdminData.StoreResponse> getStores() {
        List<Product> products = productRepository.findAllByOrderByProIdDesc();
        List<Order> orders = orderRepository.findAllByOrderByCreatedAtDesc();
        Map<Integer, User> usersById = userMap();
        Map<Integer, List<Product>> productsByStore = products.stream()
                .filter(this::isMarketplace).filter(product -> product.getStore() != null)
                .collect(Collectors.groupingBy(product -> product.getStore().getStoreId()));
        Map<Integer, List<Order>> ordersByStore = orders.stream().filter(order -> order.getStoreId() != null)
                .collect(Collectors.groupingBy(Order::getStoreId));

        return storeRepository.findAll().stream()
                .sorted(Comparator.comparing(MarketplaceStore::getSubmittedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(store -> toStoreResponse(store, productsByStore.getOrDefault(store.getStoreId(), List.of()),
                        ordersByStore.getOrDefault(store.getStoreId(), List.of()), usersById))
                .toList();
    }

    @Transactional(readOnly = true)
    public SuperAdminData.StoreResponse getStore(Integer storeId) {
        MarketplaceStore store = storeRepository.findByStoreId(storeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Store not found"));
        List<Product> products = productRepository.findByStore_StoreIdOrderByProIdDesc(storeId).stream()
                .filter(this::isMarketplace).toList();
        List<Order> orders = orderRepository.findByStoreIdOrderByCreatedAtDesc(storeId);
        return toStoreResponse(store, products, orders, userMap());
    }

    @Transactional(readOnly = true)
    public List<SuperAdminData.CatalogProductResponse> getCatalog() {
        return productRepository.findAllByOrderByProIdDesc().stream().map(this::toCatalogProduct).toList();
    }

    @Transactional(readOnly = true)
    public List<SuperAdminData.TransactionResponse> getTransactions() {
        Map<Integer, User> usersById = userMap();
        return orderRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(order -> toTransaction(order, usersById)).toList();
    }

    @Transactional(readOnly = true)
    public List<SuperAdminData.UserResponse> getUsers() {
        Map<Integer, MarketplaceStore> storesBySeller = storeRepository.findAll().stream()
                .collect(Collectors.toMap(MarketplaceStore::getSellerUserId, store -> store, (first, ignored) -> first));
        return userRepository.findAll().stream()
                .filter(user -> !isStaff(user))
                .sorted(Comparator.comparing(User::getCreateDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(user -> toUser(user, storesBySeller.get(user.getUaId()))).toList();
    }

    @Transactional(readOnly = true)
    public List<SuperAdminData.StaffResponse> getStaff() {
        return userRepository.findAll().stream().filter(this::isStaff)
                .sorted(Comparator.comparing(User::getCreateDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toStaff).toList();
    }

    @Transactional
    public SuperAdminData.StaffResponse createStaff(SuperAdminData.CreateStaffRequest request) {
        if (request == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Staff account data is required");
        String username = require(request.username(), "Username is required");
        String email = require(request.email(), "Email is required");
        String password = require(request.password(), "Password is required");
        if (password.length() < 8) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must contain at least 8 characters");
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "That username is already in use");
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "That email is already in use");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole("ADMIN");
        user.setPhone(blankToNull(request.phone()));
        user.setAddress(blankToNull(request.address()));
        return toStaff(userRepository.saveAndFlush(user));
    }

    @Transactional
    public void deleteUser(Integer userId, String actorEmail, String confirmation) {
        requireDeletionConfirmation(confirmation);
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User account not found"));
        if (isStaff(target)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use the staff endpoint to delete an administrator account");
        }
        deleteAccount(target, actorEmail);
    }

    @Transactional
    public void deleteStaff(Integer userId, String actorEmail, String confirmation) {
        requireDeletionConfirmation(confirmation);
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff account not found"));
        if (!isStaff(target)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use the user endpoint to delete this account");
        }
        if ("SUPER_ADMIN".equalsIgnoreCase(target.getRole())
                && userRepository.countByRoleIgnoreCase("SUPER_ADMIN") <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The last Super Admin account cannot be deleted");
        }
        deleteAccount(target, actorEmail);
    }

    private List<SuperAdminData.MonthlyMetricResponse> monthlyMetrics(List<Order> orders) {
        Map<YearMonth, double[]> valuesByMonth = new HashMap<>();
        for (Order order : orders) {
            if (order.getCreatedAt() == null) continue;
            YearMonth month = YearMonth.from(order.getCreatedAt());
            double[] values = valuesByMonth.computeIfAbsent(month, ignored -> new double[2]);
            values[0] += number(order.getTotalPrice());
            values[1] += 1;
        }
        LocalDate current = LocalDate.now().withDayOfMonth(1);
        List<SuperAdminData.MonthlyMetricResponse> result = new ArrayList<>();
        for (int offset = 11; offset >= 0; offset--) {
            YearMonth month = YearMonth.from(current.minusMonths(offset));
            double[] values = valuesByMonth.getOrDefault(month, new double[2]);
            result.add(new SuperAdminData.MonthlyMetricResponse(
                    month.format(DateTimeFormatter.ofPattern("MMM yy", Locale.ENGLISH)), values[0], (int) values[1]));
        }
        return result;
    }

    private List<SuperAdminData.GameCountResponse> catalogByGame(List<Product> products) {
        return products.stream().filter(this::isVisibleListing)
                .collect(Collectors.groupingBy(product -> product.getCardGame() == null || product.getCardGame().getGameName() == null
                                ? "Uncategorized" : product.getCardGame().getGameName(), Collectors.summingInt(product -> 1)))
                .entrySet().stream().sorted(Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue)
                        .reversed().thenComparing(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER)))
                .map(entry -> new SuperAdminData.GameCountResponse(entry.getKey(), entry.getValue())).toList();
    }

    private SuperAdminData.StorePerformanceResponse toStorePerformance(MarketplaceStore store, List<Product> products,
                                                                         List<Order> orders, Map<Integer, User> usersById) {
        return new SuperAdminData.StorePerformanceResponse(store.getStoreId(), store.getStoreName(), ownerName(store, usersById),
                store.getStoreStatus(), products.size(), orders.size(),
                orders.stream().mapToDouble(order -> number(order.getTotalPrice())).sum());
    }

    private SuperAdminData.StoreResponse toStoreResponse(MarketplaceStore store, List<Product> products,
                                                           List<Order> orders, Map<Integer, User> usersById) {
        int active = (int) products.stream().filter(this::isVisibleListing).count();
        int inStock = (int) products.stream().filter(this::isVisibleListing)
                .filter(product -> integer(product.getProQuantity()) > 0).count();
        int totalStock = products.stream().mapToInt(product -> integer(product.getProQuantity())).sum();
        String location = java.util.stream.Stream.of(store.getDistrict(), store.getProvince())
                .filter(value -> value != null && !value.isBlank()).collect(Collectors.joining(", "));
        return new SuperAdminData.StoreResponse(store.getStoreId(), store.getStoreName(), store.getStoreSlug(),
                store.getStoreStatus(), store.getStoreDescription(), ownerName(store, usersById), store.getOwnerEmail(),
                store.getOwnerPhone(), location.isBlank() ? null : location, store.getSubmittedAt(), store.getReviewedAt(),
                products.size(), active, inStock, totalStock, orders.size(),
                orders.stream().mapToDouble(order -> number(order.getTotalPrice())).sum(),
                products.stream().map(this::toCatalogProduct).toList());
    }

    private SuperAdminData.CatalogProductResponse toCatalogProduct(Product product) {
        MarketplaceStore store = product.getStore();
        return new SuperAdminData.CatalogProductResponse(product.getProId(), product.getProName(),
                product.getCardGame() == null ? "Uncategorized" : product.getCardGame().getGameName(), product.getProType(),
                product.getProductSet(), product.getLanguage(), product.getProCost(), product.getProPriceOfSell(),
                product.getProQuantity(), product.getProImageUrl(), product.getProDescription(), product.getIsActive(),
                product.getListingSource(), product.getApprovalStatus(), store == null ? null : store.getStoreId(),
                store == null ? "Optracard Official Store" : store.getStoreName());
    }

    private SuperAdminData.TransactionResponse toTransaction(Order order, Map<Integer, User> usersById) {
        User buyer = usersById.get(order.getUserId());
        return new SuperAdminData.TransactionResponse(order.getOrderId(), orderNumber(order), order.getCreatedAt(),
                order.getUserId(), buyer == null ? "Unknown customer" : buyer.getUsername(), order.getStoreId(),
                order.getStoreName(), order.getSource(), displayOrderStatus(order.getStatus()), order.getPaymentStatus(),
                order.getShippingMethod(), order.getTotalPrice(),
                order.getItems() == null ? 0 : order.getItems().stream().mapToInt(item -> integer(item.getQuantity())).sum());
    }

    private SuperAdminData.UserResponse toUser(User user, MarketplaceStore store) {
        return new SuperAdminData.UserResponse(user.getUaId(), user.getUsername(), user.getEmail(), user.getRole(), user.getPhone(),
                user.getAddress(), user.getCreateDate(), store == null ? null : store.getStoreId(),
                store == null ? null : store.getStoreName(), store == null ? null : store.getStoreStatus());
    }

    private SuperAdminData.StaffResponse toStaff(User user) {
        return new SuperAdminData.StaffResponse(user.getUaId(), user.getUsername(), user.getEmail(), user.getRole(), user.getPhone(),
                user.getAddress(), user.getCreateDate());
    }

    private Map<Integer, User> userMap() {
        return userRepository.findAll().stream()
                .collect(Collectors.toMap(User::getUaId, user -> user));
    }

    private void deleteAccount(User target, String actorEmail) {
        if (actorEmail != null && actorEmail.equalsIgnoreCase(target.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot delete the account you are currently using");
        }

        // Carts are account-owned and have a cascading Cart -> CartItem relationship.
        cartRepository.findByUserId(target.getUaId()).ifPresent(cartRepository::delete);

        // MarketplaceStore stores the seller id as a scalar. Remove its listings and the store as a unit
        // so deleting a seller never leaves live, ownerless marketplace inventory behind.
        for (MarketplaceStore store : storeRepository.findAllBySellerUserId(target.getUaId())) {
            productRepository.deleteAll(productRepository.findByStore_StoreIdOrderByProIdDesc(store.getStoreId()));
            storeRepository.delete(store);
        }

        // Orders intentionally remain: they are immutable transaction/audit records and only store user ids/snapshots.
        userRepository.delete(target);
        userRepository.flush();
    }

    private void requireDeletionConfirmation(String confirmation) {
        if (!"DELETE".equals(confirmation)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Type DELETE to confirm account deletion");
        }
    }

    private String ownerName(MarketplaceStore store, Map<Integer, User> usersById) {
        String submittedName = java.util.stream.Stream.of(store.getOwnerFirstName(), store.getOwnerLastName())
                .filter(value -> value != null && !value.isBlank()).collect(Collectors.joining(" "));
        if (!submittedName.isBlank()) return submittedName;
        User owner = usersById.get(store.getSellerUserId());
        return owner == null ? "Owner unavailable" : owner.getUsername();
    }

    private boolean isMarketplace(Product product) {
        return "MARKETPLACE".equalsIgnoreCase(product.getListingSource());
    }

    private boolean isVisibleListing(Product product) {
        return Boolean.TRUE.equals(product.getIsActive())
                && (!isMarketplace(product) || "APPROVED".equalsIgnoreCase(product.getApprovalStatus()));
    }

    private boolean isStaff(User user) {
        return "ADMIN".equalsIgnoreCase(user.getRole()) || "SUPER_ADMIN".equalsIgnoreCase(user.getRole());
    }

    private String displayOrderStatus(String status) {
        if (status == null || status.isBlank()) return "Processing";
        return switch (status.trim().toUpperCase(Locale.ROOT)) {
            case "SHIPPED" -> "Shipped";
            case "DELIVERED", "COMPLETED" -> "Delivered";
            case "CANCELED", "CANCELLED" -> "Canceled";
            default -> "Processing";
        };
    }

    private String orderNumber(Order order) {
        String date = order.getCreatedAt() == null ? "UNKNOWN" : order.getCreatedAt().toLocalDate()
                .format(DateTimeFormatter.BASIC_ISO_DATE);
        return "ORD-" + date + "-" + String.format("%04d", order.getOrderId());
    }

    private String require(String value, String message) {
        if (value == null || value.trim().isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        return value.trim();
    }

    private String blankToNull(String value) { return value == null || value.trim().isEmpty() ? null : value.trim(); }
    private double number(Double value) { return value == null ? 0d : value; }
    private int integer(Integer value) { return value == null ? 0 : value; }
}
