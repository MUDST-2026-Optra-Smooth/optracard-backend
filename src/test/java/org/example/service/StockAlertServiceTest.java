package org.example.service;

import org.example.dto.StockAlertResponse;
import org.example.dto.StockAlertSettingsRequest;
import org.example.model.MarketplaceStore;
import org.example.model.Product;
import org.example.model.StockAlert;
import org.example.model.User;
import org.example.repository.MarketplaceStoreRepository;
import org.example.repository.ProductRepository;
import org.example.repository.StockAlertReadRepository;
import org.example.repository.StockAlertRepository;
import org.example.repository.StockAlertSettingsRepository;
import org.example.repository.StockAlertStoreSettingsRepository;
import org.example.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockAlertServiceTest {

    @Mock private StockAlertRepository alertRepository;
    @Mock private StockAlertReadRepository readRepository;
    @Mock private StockAlertSettingsRepository settingsRepository;
    @Mock private StockAlertStoreSettingsRepository storeSettingsRepository;
    @Mock private ProductRepository productRepository;
    @Mock private MarketplaceStoreRepository storeRepository;
    @Mock private UserRepository userRepository;

    private StockAlertService service;

    @BeforeEach
    void setUp() {
        service = new StockAlertService(alertRepository, readRepository, settingsRepository, storeSettingsRepository,
                productRepository, storeRepository, userRepository);
    }

    @Test
    void sellerSeesOnlyOwnStoreLowAndOutOfStockAlerts() {
        User seller = user(7, "SELLER");
        MarketplaceStore ownStore = store(11, 7, "Own Store", "APPROVED");
        Product low = product(1, "Low product", 2, "MARKETPLACE", ownStore, true, "APPROVED");
        Product out = product(2, "Out product", 0, "MARKETPLACE", ownStore, true, "APPROVED");
        Product anotherStore = product(3, "Private product", 1, "MARKETPLACE",
                store(12, 8, "Another Store", "APPROVED"), true, "APPROVED");

        when(userRepository.findByEmail("seller@example.com")).thenReturn(Optional.of(seller));
        when(storeRepository.findFirstBySellerUserIdOrderByStoreIdDesc(7)).thenReturn(Optional.of(ownStore));
        when(productRepository.findByStore_StoreIdOrderByProIdDesc(11)).thenReturn(List.of(low, out));
        when(storeSettingsRepository.findById(11)).thenReturn(Optional.empty());
        when(alertRepository.findByProductId(anyInt())).thenReturn(Optional.empty());
        when(readRepository.findByUserIdAndAlertIdIn(anyInt(), any())).thenReturn(List.of());
        when(alertRepository.findByResolvedAtIsNullAndProductIdIn(any())).thenReturn(List.of(
                activeAlert(101, 1, StockAlert.LOW_STOCK, 2),
                activeAlert(102, 2, StockAlert.OUT_OF_STOCK, 0)
        ));

        List<StockAlertResponse> alerts = service.getAlerts("seller@example.com");

        assertEquals(Set.of(1, 2), alerts.stream().map(StockAlertResponse::productId).collect(java.util.stream.Collectors.toSet()));
        assertTrue(alerts.stream().noneMatch(alert -> alert.productId().equals(anotherStore.getProId())));
        assertEquals(5, alerts.get(0).threshold());
    }

    @Test
    void adminSeesOfficialAlertsOnly() {
        User admin = user(1, "ADMIN");
        MarketplaceStore store = store(11, 7, "Approved Store", "APPROVED");
        Product official = product(1, "Official", 1, "OFFICIAL", null, true, "APPROVED");
        Product marketplace = product(2, "Marketplace", 3, "MARKETPLACE", store, true, "APPROVED");
        Product pending = product(3, "Pending", 1, "MARKETPLACE", store, true, "PENDING");
        Product inactive = product(4, "Inactive", 1, "OFFICIAL", null, false, "APPROVED");

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(productRepository.findAllByOrderByProIdDesc()).thenReturn(List.of(official, marketplace, pending, inactive));
        when(settingsRepository.findById(1)).thenReturn(Optional.empty());
        when(alertRepository.findByProductId(anyInt())).thenReturn(Optional.empty());
        when(readRepository.findByUserIdAndAlertIdIn(anyInt(), any())).thenReturn(List.of());
        when(alertRepository.findByResolvedAtIsNullAndProductIdIn(any())).thenReturn(List.of(
                activeAlert(201, 1, StockAlert.LOW_STOCK, 1)
        ));

        List<StockAlertResponse> alerts = service.getAlerts("admin@example.com");

        assertEquals(Set.of(1), alerts.stream().map(StockAlertResponse::productId).collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void sellerCanUpdateOnlyOwnStoreThreshold() {
        User seller = user(7, "SELLER");
        MarketplaceStore ownStore = store(11, 7, "Own Store", "APPROVED");
        when(userRepository.findByEmail("seller@example.com")).thenReturn(Optional.of(seller));
        when(storeRepository.findFirstBySellerUserIdOrderByStoreIdDesc(7)).thenReturn(Optional.of(ownStore));

        when(storeSettingsRepository.findById(11)).thenReturn(Optional.empty());

        assertEquals(10, service.updateSettings("seller@example.com", new StockAlertSettingsRequest(10)).defaultThreshold());
        ArgumentCaptor<org.example.model.StockAlertStoreSettings> sellerCaptor = ArgumentCaptor.forClass(org.example.model.StockAlertStoreSettings.class);
        verify(storeSettingsRepository).save(sellerCaptor.capture());
        assertEquals(11, sellerCaptor.getValue().getStoreId());
        assertEquals(10, sellerCaptor.getValue().getDefaultThreshold());
    }

    @Test
    void superAdminCannotAccessStockAlerts() {
        User superAdmin = user(99, "SUPER_ADMIN");
        when(userRepository.findByEmail("super_admin@example.com")).thenReturn(Optional.of(superAdmin));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.getAlerts("super_admin@example.com"));

        assertEquals(HttpStatus.FORBIDDEN.value(), exception.getStatusCode().value());
    }

    @Test
    void adminUpdatesOfficialStoreThreshold() {

        User admin = user(1, "ADMIN");
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(settingsRepository.findById(1)).thenReturn(Optional.empty());

        assertEquals(10, service.updateSettings("admin@example.com", new StockAlertSettingsRequest(10)).defaultThreshold());
        ArgumentCaptor<org.example.model.StockAlertSettings> captor = ArgumentCaptor.forClass(org.example.model.StockAlertSettings.class);
        verify(settingsRepository).save(captor.capture());
        assertEquals(10, captor.getValue().getDefaultThreshold());
    }

    @Test
    void markingAlertReadChecksSellerStoreOwnership() {
        User seller = user(7, "SELLER");
        MarketplaceStore ownStore = store(11, 7, "Own Store", "APPROVED");
        MarketplaceStore otherStore = store(12, 8, "Other Store", "APPROVED");
        Product product = product(1, "Other product", 0, "MARKETPLACE", otherStore, true, "APPROVED");
        StockAlert alert = activeAlert(301, 1, StockAlert.OUT_OF_STOCK, 0);

        when(userRepository.findByEmail("seller@example.com")).thenReturn(Optional.of(seller));
        when(alertRepository.findById(301)).thenReturn(Optional.of(alert));
        when(productRepository.findByProId(1)).thenReturn(Optional.of(product));
        when(storeRepository.findFirstBySellerUserIdOrderByStoreIdDesc(7)).thenReturn(Optional.of(ownStore));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.markRead("seller@example.com", 301, true));

        assertEquals(HttpStatus.FORBIDDEN.value(), exception.getStatusCode().value());
    }

    private User user(int id, String role) {
        User user = new User();
        user.setUaId(id);
        user.setEmail(role.toLowerCase() + "@example.com");
        user.setRole(role);
        return user;
    }

    private MarketplaceStore store(int id, int sellerUserId, String name, String status) {
        MarketplaceStore store = new MarketplaceStore();
        store.setStoreId(id);
        store.setSellerUserId(sellerUserId);
        store.setStoreName(name);
        store.setStoreSlug(name.toLowerCase().replace(' ', '-'));
        store.setStoreStatus(status);
        return store;
    }

    private Product product(int id, String name, int quantity, String source, MarketplaceStore store,
                            boolean active, String approval) {
        Product product = new Product();
        product.setProId(id);
        product.setProName(name);
        product.setProQuantity(quantity);
        product.setListingSource(source);
        product.setStore(store);
        product.setIsActive(active);
        product.setApprovalStatus(approval);
        return product;
    }

    private StockAlert activeAlert(int id, int productId, String type, int stock) {
        StockAlert alert = new StockAlert();
        alert.setId(id);
        alert.setProductId(productId);
        alert.setAlertType(type);
        alert.setCurrentStock(stock);
        alert.setThreshold(5);
        alert.setActivatedAt(LocalDateTime.now().minusMinutes(1));
        alert.setUpdatedAt(LocalDateTime.now());
        return alert;
    }
}
