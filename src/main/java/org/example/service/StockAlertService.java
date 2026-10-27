package org.example.service;

import org.example.dto.StockAlertResponse;
import org.example.dto.StockAlertSettingsRequest;
import org.example.dto.StockAlertSettingsResponse;
import org.example.model.MarketplaceStore;
import org.example.model.Product;
import org.example.model.StockAlert;
import org.example.model.StockAlertRead;
import org.example.model.StockAlertSettings;
import org.example.model.StockAlertStoreSettings;
import org.example.model.User;
import org.example.repository.MarketplaceStoreRepository;
import org.example.repository.ProductRepository;
import org.example.repository.StockAlertReadRepository;
import org.example.repository.StockAlertRepository;
import org.example.repository.StockAlertSettingsRepository;
import org.example.repository.StockAlertStoreSettingsRepository;
import org.example.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class StockAlertService {
    private static final int MAX_THRESHOLD = 100_000;

    private final StockAlertRepository alertRepository;
    private final StockAlertReadRepository readRepository;
    private final StockAlertSettingsRepository settingsRepository;
    private final StockAlertStoreSettingsRepository storeSettingsRepository;
    private final ProductRepository productRepository;
    private final MarketplaceStoreRepository storeRepository;
    private final UserRepository userRepository;

    public StockAlertService(StockAlertRepository alertRepository,
                             StockAlertReadRepository readRepository,
                             StockAlertSettingsRepository settingsRepository,
                             StockAlertStoreSettingsRepository storeSettingsRepository,
                             ProductRepository productRepository,
                             MarketplaceStoreRepository storeRepository,
                             UserRepository userRepository) {
        this.alertRepository = alertRepository;
        this.readRepository = readRepository;
        this.settingsRepository = settingsRepository;
        this.storeSettingsRepository = storeSettingsRepository;
        this.productRepository = productRepository;
        this.storeRepository = storeRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public List<StockAlertResponse> getAlerts(String email) {
        User user = currentUser(email);
        List<Product> products = accessibleProducts(user);
        syncProducts(products, thresholdFor(user));
        return toResponses(user, products);
    }

    @Transactional
    public void markRead(String email, Integer alertId, boolean read) {
        User user = currentUser(email);
        StockAlert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stock alert not found"));
        Product product = productRepository.findByProId(alert.getProductId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        requireProductAccess(user, product);
        if (read) {
            StockAlertRead state = readRepository.findByAlertIdAndUserId(alertId, user.getUaId())
                    .orElseGet(StockAlertRead::new);
            state.setAlertId(alertId);
            state.setUserId(user.getUaId());
            state.setReadAt(LocalDateTime.now());
            readRepository.save(state);
        } else {
            readRepository.deleteByAlertIdAndUserId(alertId, user.getUaId());
        }
    }

    @Transactional
    public void markAllRead(String email) {
        User user = currentUser(email);
        List<Product> products = accessibleProducts(user);
        syncProducts(products, thresholdFor(user));
        List<StockAlertResponse> alerts = toResponses(user, products);
        LocalDateTime now = LocalDateTime.now();
        for (StockAlertResponse response : alerts) {
            if (response.read()) continue;
            StockAlertRead state = readRepository.findByAlertIdAndUserId(response.id(), user.getUaId())
                    .orElseGet(StockAlertRead::new);
            state.setAlertId(response.id());
            state.setUserId(user.getUaId());
            state.setReadAt(now);
            readRepository.save(state);
        }
    }

    @Transactional(readOnly = true)
    public StockAlertSettingsResponse getSettings(String email) {
        return new StockAlertSettingsResponse(thresholdFor(currentUser(email)));
    }

    @Transactional
    public StockAlertSettingsResponse updateSettings(String email, StockAlertSettingsRequest request) {
        User user = currentUser(email);
        int threshold = validateThreshold(request == null ? null : request.defaultThreshold());
        String role = roleOf(user);
        if ("ADMIN".equals(role)) {
            StockAlertSettings settings = settingsRepository.findById(StockAlertSettings.SETTINGS_ID)
                    .orElseGet(() -> {
                        StockAlertSettings created = new StockAlertSettings();
                        created.setId(StockAlertSettings.SETTINGS_ID);
                        return created;
                    });
            settings.setDefaultThreshold(threshold);
            settingsRepository.save(settings);
        } else if ("SELLER".equals(role)) {
            MarketplaceStore store = approvedStore(user);
            StockAlertStoreSettings settings = storeSettingsRepository.findById(store.getStoreId())
                    .orElseGet(() -> {
                        StockAlertStoreSettings created = new StockAlertStoreSettings();
                        created.setStoreId(store.getStoreId());
                        return created;
                    });
            settings.setDefaultThreshold(threshold);
            storeSettingsRepository.save(settings);
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Stock alert settings are not available for this account");
        }
        return new StockAlertSettingsResponse(threshold);
    }

    private List<StockAlertResponse> toResponses(User user, List<Product> products) {
        if (products.isEmpty()) return List.of();
        Map<Integer, Product> productsById = products.stream()
                .filter(product -> product.getProId() != null)
                .collect(java.util.stream.Collectors.toMap(Product::getProId, product -> product));
        List<Integer> productIds = List.copyOf(productsById.keySet());
        List<StockAlert> activeAlerts = alertRepository.findByResolvedAtIsNullAndProductIdIn(productIds);
        if (activeAlerts.isEmpty()) return List.of();
        Map<Integer, StockAlertRead> readsByAlertId = new HashMap<>();
        List<Integer> alertIds = activeAlerts.stream().map(StockAlert::getId).toList();
        for (StockAlertRead read : readRepository.findByUserIdAndAlertIdIn(user.getUaId(), alertIds)) {
            readsByAlertId.put(read.getAlertId(), read);
        }
        return activeAlerts.stream()
                .sorted(Comparator.comparing(StockAlert::getActivatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(alert -> {
                    Product product = productsById.get(alert.getProductId());
                    StockAlertRead read = readsByAlertId.get(alert.getId());
                    boolean isRead = read != null && read.getReadAt() != null && alert.getActivatedAt() != null
                            && !read.getReadAt().isBefore(alert.getActivatedAt());
                    return toResponse(alert, product, isRead);
                })
                .toList();
    }

    private StockAlertResponse toResponse(StockAlert alert, Product product, boolean read) {
        MarketplaceStore store = product.getStore();
        String source = product.getListingSource() == null ? "OFFICIAL" : product.getListingSource().toUpperCase();
        return new StockAlertResponse(alert.getId(), product.getProId(), product.getProName(), alert.getAlertType(),
                alert.getCurrentStock(), alert.getThreshold(), source,
                store == null ? null : store.getStoreId(),
                store == null ? "Optracard Official Store" : store.getStoreName(),
                alert.getActivatedAt(), alert.getUpdatedAt(), read);
    }

    private void syncProducts(Collection<Product> products, int threshold) {
        for (Product product : products) syncProduct(product, threshold);
    }

    private void syncProduct(Product product, int threshold) {
        if (product.getProId() == null) return;
        int stock = integer(product.getProQuantity());
        String currentType = stock == 0 ? StockAlert.OUT_OF_STOCK
                : stock <= threshold ? StockAlert.LOW_STOCK : null;
        StockAlert alert = alertRepository.findByProductId(product.getProId()).orElse(null);
        if (currentType == null) {
            if (alert != null && alert.getResolvedAt() == null) {
                alert.setResolvedAt(LocalDateTime.now());
                alertRepository.save(alert);
            }
            return;
        }

        boolean newActivation = alert == null || alert.getResolvedAt() != null
                || !currentType.equalsIgnoreCase(alert.getAlertType());
        if (alert == null) {
            alert = new StockAlert();
            alert.setProductId(product.getProId());
        }
        if (newActivation) {
            alert.setActivatedAt(LocalDateTime.now());
            alert.setResolvedAt(null);
        }
        alert.setAlertType(currentType);
        alert.setCurrentStock(stock);
        alert.setThreshold(threshold);
        alertRepository.save(alert);
    }

    private List<Product> accessibleProducts(User user) {
        String role = roleOf(user);
        if ("SELLER".equals(role)) {
            MarketplaceStore store = approvedStore(user);
            return productRepository.findByStore_StoreIdOrderByProIdDesc(store.getStoreId());
        }
        if ("ADMIN".equals(role)) {
            return productRepository.findAllByOrderByProIdDesc().stream()
                    .filter(this::isAdminVisible)
                    .toList();
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Stock alerts are not available for this account");
    }

    private boolean isAdminVisible(Product product) {
        return Boolean.TRUE.equals(product.getIsActive())
                && !"MARKETPLACE".equalsIgnoreCase(product.getListingSource());
    }

    private void requireProductAccess(User user, Product product) {
        String role = roleOf(user);
        if ("ADMIN".equals(role)) {
            if (!isAdminVisible(product)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Stock alert is not available for this product");
            return;
        }
        if (!"SELLER".equals(role) || product.getStore() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Stock alert is not available for this account");
        }
        MarketplaceStore store = approvedStore(user);
        if (!store.getStoreId().equals(product.getStore().getStoreId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Stock alert is not available for this product");
        }
    }

    private User currentUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private int defaultThreshold() {
        return settingsRepository.findById(StockAlertSettings.SETTINGS_ID)
                .map(StockAlertSettings::getDefaultThreshold)
                .filter(value -> value != null && value >= 1)
                .orElse(StockAlertSettings.DEFAULT_THRESHOLD);
    }

    private int thresholdFor(User user) {
        String role = roleOf(user);
        if ("ADMIN".equals(role)) return defaultThreshold();
        if ("SELLER".equals(role)) {
            MarketplaceStore store = approvedStore(user);
            return storeSettingsRepository.findById(store.getStoreId())
                    .map(StockAlertStoreSettings::getDefaultThreshold)
                    .filter(value -> value != null && value >= 1)
                    .orElse(StockAlertStoreSettings.DEFAULT_THRESHOLD);
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Stock alerts are not available for this account");
    }

    private MarketplaceStore approvedStore(User user) {
        MarketplaceStore store = storeRepository.findFirstBySellerUserIdOrderByStoreIdDesc(user.getUaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "An approved seller store is required"));
        if (!"APPROVED".equalsIgnoreCase(store.getStoreStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Your seller store is not approved yet");
        }
        return store;
    }

    private String roleOf(User user) {
        return user.getRole() == null ? "" : user.getRole().toUpperCase();
    }

    private int validateThreshold(Integer value) {
        if (value == null || value < 1 || value > MAX_THRESHOLD) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Default stock alert threshold must be between 1 and " + MAX_THRESHOLD);
        }
        return value;
    }

    private int integer(Integer value) { return value == null ? 0 : value; }
}
