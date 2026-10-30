package org.example.service;

import org.example.dto.SuperAdminData;
import org.example.model.*;
import org.example.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SuperAdminDataServiceTest {

    @Mock private MarketplaceStoreRepository storeRepository;
    @Mock private ProductRepository productRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private CartRepository cartRepository;
    @Mock private StockAlertSettingsRepository stockAlertSettingsRepository;

    private SuperAdminDataService service;

    @BeforeEach
    void setUp() {
        service = new SuperAdminDataService(
                storeRepository,
                productRepository,
                orderRepository,
                userRepository,
                passwordEncoder,
                cartRepository,
                stockAlertSettingsRepository
        );
    }

    @Test
    void getOverview_calculatesMetricsCorrectly() {
        MarketplaceStore approvedStore = new MarketplaceStore();
        approvedStore.setStoreId(1);
        approvedStore.setStoreStatus("APPROVED");
        approvedStore.setStoreName("Card Shop A");

        MarketplaceStore pendingStore = new MarketplaceStore();
        pendingStore.setStoreId(2);
        pendingStore.setStoreStatus("PENDING");
        pendingStore.setStoreName("Card Shop B");

        when(storeRepository.findAll()).thenReturn(List.of(approvedStore, pendingStore));

        Product productInStock = new Product();
        productInStock.setProId(10);
        productInStock.setProQuantity(10);
        productInStock.setIsActive(true);
        productInStock.setListingSource("OFFICIAL");

        Product productLowStock = new Product();
        productLowStock.setProId(11);
        productLowStock.setProQuantity(3);
        productLowStock.setIsActive(true);
        productLowStock.setListingSource("OFFICIAL");

        Product productOutOfStock = new Product();
        productOutOfStock.setProId(12);
        productOutOfStock.setProQuantity(0);
        productOutOfStock.setIsActive(true);
        productOutOfStock.setListingSource("OFFICIAL");

        when(productRepository.findAllByOrderByProIdDesc())
                .thenReturn(List.of(productInStock, productLowStock, productOutOfStock));

        Order order = new Order();
        order.setOrderId(100);
        order.setTotalPrice(1500.0);
        order.setCreatedAt(LocalDateTime.now().minusDays(2));
        order.setStoreId(1);

        when(orderRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(order));
        when(userRepository.findAll()).thenReturn(List.of());

        SuperAdminData.OverviewResponse overview = service.getOverview();

        assertNotNull(overview);
        assertEquals(2, overview.totalStores());
        assertEquals(1, overview.approvedStores());
        assertEquals(1, overview.pendingStores());
        assertEquals(3, overview.listedProducts());
        assertEquals(3, overview.activeListings());
        assertEquals(1500.0, overview.gmvLast30Days());
        assertEquals(1, overview.ordersLast30Days());
        assertEquals(1, overview.inStockListings());
        assertEquals(1, overview.lowStockListings());
        assertEquals(1, overview.outOfStockListings());
    }

    @Test
    void getStores_returnsMappedStores() {
        MarketplaceStore store = new MarketplaceStore();
        store.setStoreId(1);
        store.setStoreName("Trading Card Hub");
        store.setSubmittedAt(LocalDateTime.now());
        when(storeRepository.findAll()).thenReturn(List.of(store));
        when(productRepository.findAllByOrderByProIdDesc()).thenReturn(List.of());
        when(orderRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());
        when(userRepository.findAll()).thenReturn(List.of());

        List<SuperAdminData.StoreResponse> stores = service.getStores();

        assertEquals(1, stores.size());
        assertEquals("Trading Card Hub", stores.get(0).storeName());
    }

    @Test
    void createStaff_withValidRequest_encodesPasswordAndAssignsAdminRole() {
        SuperAdminData.CreateStaffRequest req = new SuperAdminData.CreateStaffRequest(
                "newstaff", "staff@optracard.com", "SecurePass123!", "0899999999", "HQ"
        );
        when(userRepository.existsByUsernameIgnoreCase("newstaff")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("staff@optracard.com")).thenReturn(false);
        when(passwordEncoder.encode("SecurePass123!")).thenReturn("encoded_pass");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        SuperAdminData.StaffResponse response = service.createStaff(req);

        assertNotNull(response);
        assertEquals("newstaff", response.username());
        assertEquals("staff@optracard.com", response.email());
        assertEquals("ADMIN", response.role());
    }

    @Test
    void createStaff_withShortPassword_throwsBadRequest() {
        SuperAdminData.CreateStaffRequest req = new SuperAdminData.CreateStaffRequest(
                "newstaff", "staff@optracard.com", "short", "0899999999", "HQ"
        );

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.createStaff(req));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("8 characters"));
    }

    @Test
    void createStaff_withDuplicateEmail_throwsConflict() {
        SuperAdminData.CreateStaffRequest req = new SuperAdminData.CreateStaffRequest(
                "newstaff", "existing@optracard.com", "ValidPass123", null, null
        );
        when(userRepository.existsByEmailIgnoreCase("existing@optracard.com")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.createStaff(req));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void deleteStaff_whenDeletingLastSuperAdmin_throwsConflict() {
        User superAdmin = new User();
        superAdmin.setUaId(1);
        superAdmin.setRole("SUPER_ADMIN");

        when(userRepository.findById(1)).thenReturn(Optional.of(superAdmin));
        when(userRepository.countByRoleIgnoreCase("SUPER_ADMIN")).thenReturn(1L);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.deleteStaff(1, "actor@optracard.com", "DELETE"));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("last Super Admin"));
    }

    @Test
    void deleteStaff_withoutExactConfirmation_throwsBadRequest() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.deleteStaff(1, "actor@optracard.com", "NO"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void deleteUser_whenTargetIsStaff_throwsBadRequest() {
        User staff = new User();
        staff.setUaId(2);
        staff.setRole("ADMIN");

        when(userRepository.findById(2)).thenReturn(Optional.of(staff));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.deleteUser(2, "actor@optracard.com", "DELETE"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("staff endpoint"));
    }

    @Test
    void deleteUser_whenRegularUser_deletesSuccessfully() {
        User regularUser = new User();
        regularUser.setUaId(5);
        regularUser.setEmail("user5@optracard.com");
        regularUser.setRole("USER");

        Cart cart = new Cart();
        cart.setCartId(1);
        cart.setUserId(5);

        when(userRepository.findById(5)).thenReturn(Optional.of(regularUser));
        when(cartRepository.findByUserId(5)).thenReturn(Optional.of(cart));
        when(storeRepository.findAllBySellerUserId(5)).thenReturn(List.of());

        service.deleteUser(5, "actor@optracard.com", "DELETE");

        verify(cartRepository).delete(cart);
        verify(userRepository).delete(regularUser);
        verify(userRepository).flush();
    }
}
