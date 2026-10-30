package org.example.service;

import org.example.dto.AdminData;
import org.example.model.CardGame;
import org.example.model.MarketplaceStore;
import org.example.model.Order;
import org.example.model.Product;
import org.example.repository.CardGameRepository;
import org.example.repository.MarketplaceStoreRepository;
import org.example.repository.OrderRepository;
import org.example.repository.ProductRepository;
import org.example.repository.StockAlertSettingsRepository;
import org.example.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminDataServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private MarketplaceStoreRepository storeRepository;
    @Mock private CardGameRepository cardGameRepository;
    @Mock private UserRepository userRepository;
    @Mock private OrderNotificationService notificationService;
    @Mock private ProductDeletionService productDeletionService;
    @Mock private StockAlertSettingsRepository stockAlertSettingsRepository;

    private AdminDataService adminDataService;

    @BeforeEach
    void setUp() {
        adminDataService = new AdminDataService(productRepository, orderRepository, storeRepository,
                cardGameRepository, userRepository, notificationService, productDeletionService,
                stockAlertSettingsRepository);
    }

    @Test
    void getDashboard_returnsAggregatedMetrics() {
        Product officialProduct = new Product();
        officialProduct.setProId(1);
        officialProduct.setListingSource("OFFICIAL");
        officialProduct.setIsActive(true);
        officialProduct.setProQuantity(4);
        officialProduct.setProCost(20.0);

        when(productRepository.findAllByOrderByProIdDesc()).thenReturn(List.of(officialProduct));
        when(userRepository.findAll()).thenReturn(List.of());

        Order order = new Order();
        order.setOrderId(10);
        order.setTotalPrice(100.0);
        order.setCreatedAt(LocalDateTime.now());
        order.setItems(List.of());
        when(orderRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(order));
        when(storeRepository.findAll()).thenReturn(List.of());

        AdminData.DashboardResponse dashboard = adminDataService.getDashboard();

        assertNotNull(dashboard);
        assertEquals(100.0, dashboard.totalRevenue());
        assertEquals(1, dashboard.totalOrders());
        assertEquals(1, dashboard.activeOfficialProducts());
        assertEquals(1, dashboard.lowStockProducts());
    }

    @Test
    void getOfficialProducts_returnsOnlyOfficialProducts() {
        Product official = new Product();
        official.setProId(1);
        official.setListingSource("OFFICIAL");

        Product marketplace = new Product();
        marketplace.setProId(2);
        marketplace.setListingSource("MARKETPLACE");

        when(productRepository.findAllByOrderByProIdDesc()).thenReturn(List.of(official, marketplace));

        List<AdminData.ProductResponse> results = adminDataService.getOfficialProducts();

        assertEquals(1, results.size());
        assertEquals(1, results.get(0).id());
    }

    @Test
    void getOfficialProduct_whenNotFound_throwsNotFound() {
        when(productRepository.findByProId(999)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> adminDataService.getOfficialProduct(999));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void createOfficialProduct_savesOfficialProductWithApprovedStatus() {
        CardGame game = new CardGame();
        game.setGameId(1);
        game.setGameName("Pokemon");

        when(cardGameRepository.findByGameNameIgnoreCase("Pokemon")).thenReturn(Optional.of(game));
        when(productRepository.saveAndFlush(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminData.ProductRequest request = new AdminData.ProductRequest(
                "Pikachu EX", "Pokemon", "Single", 50.0, 100.0, 10,
                "https://example.com/pika.jpg", "Base Set", "English", "Description"
        );

        AdminData.ProductResponse response = adminDataService.createOfficialProduct(request);

        assertNotNull(response);
        assertEquals("Pikachu EX", response.name());
        assertEquals("OFFICIAL", response.listingSource());
        assertEquals("APPROVED", response.approvalStatus());
    }

    @Test
    void updateOfficialProductActive_changesOnlyTheOfficialProductVisibility() {
        Product product = new Product();
        product.setProId(12);
        product.setProName("Official card");
        product.setListingSource("OFFICIAL");
        product.setIsActive(true);

        when(productRepository.findByProId(12)).thenReturn(Optional.of(product));
        when(productRepository.saveAndFlush(product)).thenReturn(product);

        var result = adminDataService.updateOfficialProductActive(12, false);

        assertFalse(result.active());
        verify(productRepository).saveAndFlush(product);
    }

    @Test
    void shippingAnAdminManagedOrder_requiresTrackingAndNotifiesTheBuyer() {
        Order order = new Order();
        order.setOrderId(44);
        order.setUserId(30);
        order.setStatus("Processing");
        order.setTotalPrice(100d);
        order.setItems(List.of());
        when(orderRepository.findById(44)).thenReturn(Optional.of(order));
        when(orderRepository.saveAndFlush(order)).thenReturn(order);
        when(productRepository.findAllByOrderByProIdDesc()).thenReturn(List.of());
        when(userRepository.findAll()).thenReturn(List.of());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> adminDataService.updateOrderStatus(44, "SHIPPED", " "));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

        var result = adminDataService.updateOrderStatus(44, "SHIPPED", " TH123456789 ");

        assertEquals("Shipped", result.status());
        assertEquals("TH123456789", result.trackingNumber());
        verify(notificationService).createShippingNotification(order);
    }

    @Test
    void getOrders_filtersBySourceAndTreatsMissingSourceAsOfficial() {
        Order official = new Order();
        official.setOrderId(1);
        official.setItems(List.of());

        Order marketplace = new Order();
        marketplace.setOrderId(2);
        marketplace.setSource("MARKETPLACE");
        marketplace.setItems(List.of());

        when(orderRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(official, marketplace));
        when(productRepository.findAllByOrderByProIdDesc()).thenReturn(List.of());
        when(userRepository.findAll()).thenReturn(List.of());

        assertEquals(List.of(1), adminDataService.getOrders("OFFICIAL").stream()
                .map(AdminData.OrderResponse::id).toList());
        assertEquals(List.of(2), adminDataService.getOrders("MARKETPLACE").stream()
                .map(AdminData.OrderResponse::id).toList());
        assertEquals(2, adminDataService.getOrders("ALL").size());
    }

    @Test
    void getOrders_includesShippingFeeAndSubtotalForTheOrderSummary() {
        org.example.model.OrderItem item = new org.example.model.OrderItem();
        item.setProductId(9);
        item.setQuantity(2);
        item.setPrice(100d);
        Order order = new Order();
        order.setOrderId(5);
        order.setShippingFee(50d);
        order.setTotalPrice(250d);
        order.setItems(List.of(item));
        when(orderRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(order));
        when(productRepository.findAllByOrderByProIdDesc()).thenReturn(List.of());
        when(userRepository.findAll()).thenReturn(List.of());

        AdminData.OrderResponse result = adminDataService.getOrders("ALL").get(0);

        assertEquals(50d, result.shippingFee());
        assertEquals(200d, result.subtotal());
        assertEquals(0d, result.discountAmount());
        assertNull(result.discountCode());
    }

    @Test
    void marketplaceOrders_areReadOnlyForAdministrators() {
        Order order = new Order();
        order.setOrderId(44);
        order.setSource("MARKETPLACE");
        when(orderRepository.findById(44)).thenReturn(Optional.of(order));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> adminDataService.updateOrderStatus(44, "SHIPPED", "TH123456789"));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(orderRepository, never()).saveAndFlush(any(Order.class));
    }

    @Test
    void deleteOfficialProduct_delegatesPermanentRemovalToTheDeletionService() {
        Product product = new Product();
        product.setProId(12);
        product.setListingSource("OFFICIAL");
        when(productRepository.findByProId(12)).thenReturn(Optional.of(product));

        adminDataService.deleteOfficialProduct(12);

        verify(productDeletionService).permanentlyDelete(product);
    }

    @Test
    void getCardGames_returnsAllGames() {
        CardGame game = new CardGame();
        game.setGameId(1);
        game.setGameName("One Piece");
        when(cardGameRepository.findAllByOrderByGameNameAsc()).thenReturn(List.of(game));

        List<AdminData.CardGameResponse> games = adminDataService.getCardGames();

        assertEquals(1, games.size());
        assertEquals("One Piece", games.get(0).name());
    }

    @Test
    void getStores_filtersByStatus() {
        MarketplaceStore approvedStore = new MarketplaceStore();
        approvedStore.setStoreId(1);
        approvedStore.setStoreStatus("APPROVED");

        MarketplaceStore pendingStore = new MarketplaceStore();
        pendingStore.setStoreId(2);
        pendingStore.setStoreStatus("PENDING");

        when(storeRepository.findAll()).thenReturn(List.of(approvedStore, pendingStore));
        when(productRepository.findByListingSourceOrderByProIdDesc("MARKETPLACE")).thenReturn(List.of());

        List<AdminData.StoreResponse> pendingOnly = adminDataService.getStores("PENDING");

        assertEquals(1, pendingOnly.size());
        assertEquals(2, pendingOnly.get(0).storeId());
    }
}
