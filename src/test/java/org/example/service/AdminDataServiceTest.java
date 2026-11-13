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

    @Test
    void officialProductUpdates_validateRequestAndVisibility() {
        CardGame game = new CardGame();
        game.setGameId(4);
        game.setGameName("Pokemon");
        Product product = new Product();
        product.setProId(4);
        product.setListingSource("OFFICIAL");
        when(productRepository.findByProId(4)).thenReturn(Optional.of(product));
        when(cardGameRepository.findByGameNameIgnoreCase("Pokemon")).thenReturn(Optional.of(game));
        when(productRepository.saveAndFlush(product)).thenReturn(product);

        AdminData.ProductRequest request = new AdminData.ProductRequest(
                " Updated card ", " Pokemon ", " Single ", 0d, 20d, 0,
                " ", " Set ", " English ", " Description "
        );
        AdminData.ProductResponse updated = adminDataService.updateOfficialProduct(4, request);
        assertEquals("Updated card", updated.name());
        assertEquals("OFFICIAL", updated.listingSource());
        assertNull(updated.imageUrl());
        assertEquals("Set", updated.productSet());

        assertFalse(adminDataService.updateOfficialProductActive(4, false).active());
        assertThrows(ResponseStatusException.class, () -> adminDataService.updateOfficialProductActive(4, null));
        verify(productRepository, atLeast(2)).saveAndFlush(product);
    }

    @Test
    void officialProductValidation_rejectsMalformedRequestsAndNonOfficialProducts() {
        Product marketplace = new Product();
        marketplace.setProId(8);
        marketplace.setListingSource("MARKETPLACE");
        when(productRepository.findByProId(8)).thenReturn(Optional.of(marketplace));
        assertThrows(ResponseStatusException.class, () -> adminDataService.updateOfficialProduct(8, validRequest()));
        assertThrows(ResponseStatusException.class, () -> adminDataService.updateOfficialProduct(999, validRequest()));
        assertThrows(ResponseStatusException.class, () -> adminDataService.updateOfficialProductActive(8, true));

        assertBadRequest(() -> adminDataService.createOfficialProduct(null));
        assertBadRequest(() -> adminDataService.createOfficialProduct(new AdminData.ProductRequest(
                " ", "Pokemon", "Single", 1d, 1d, 1, null, null, null, null)));
        assertBadRequest(() -> adminDataService.createOfficialProduct(new AdminData.ProductRequest(
                "Card", "Pokemon", " ", 1d, 1d, 1, null, null, null, null)));
        assertBadRequest(() -> adminDataService.createOfficialProduct(new AdminData.ProductRequest(
                "Card", "Pokemon", "Single", -1d, 1d, 1, null, null, null, null)));
        assertBadRequest(() -> adminDataService.createOfficialProduct(new AdminData.ProductRequest(
                "Card", "Pokemon", "Single", 1d, -1d, 1, null, null, null, null)));
        assertBadRequest(() -> adminDataService.createOfficialProduct(new AdminData.ProductRequest(
                "Card", "Pokemon", "Single", 1d, 1d, -1, null, null, null, null)));
        when(cardGameRepository.findByGameNameIgnoreCase("Pokemon")).thenReturn(Optional.empty());
        assertBadRequest(() -> adminDataService.createOfficialProduct(validRequest()));
    }

    @Test
    void orderAndStoreLookups_coverFoundMissingAndSourceValidation() {
        Order order = new Order();
        order.setOrderId(11);
        order.setUserId(21);
        order.setStatus("DELIVERED");
        order.setItems(List.of());
        when(orderRepository.findById(11)).thenReturn(Optional.of(order));
        when(productRepository.findAllByOrderByProIdDesc()).thenReturn(List.of());
        when(userRepository.findAll()).thenReturn(List.of());
        assertEquals("Delivered", adminDataService.getOrder(11).status());
        assertThrows(ResponseStatusException.class, () -> adminDataService.getOrder(404));
        assertThrows(ResponseStatusException.class, () -> adminDataService.getOrders("unexpected"));
        assertEquals(0, adminDataService.getOrders(null).size());

        MarketplaceStore store = new MarketplaceStore();
        store.setStoreId(5);
        store.setStoreStatus("APPROVED");
        when(storeRepository.findByStoreId(5)).thenReturn(Optional.of(store));
        when(storeRepository.findAll()).thenReturn(List.of(store));
        when(productRepository.findByListingSourceOrderByProIdDesc("MARKETPLACE")).thenReturn(List.of());
        assertEquals(5, adminDataService.getStore(5).storeId());
        assertThrows(ResponseStatusException.class, () -> adminDataService.getStore(404));
        assertEquals(1, adminDataService.getStores(null).size());
        assertEquals(1, adminDataService.getStores("ALL").size());
        assertEquals(1, adminDataService.getStores(" ").size());
    }

    @Test
    void marketplaceProducts_filterByStoreAndRejectWrongSource() {
        MarketplaceStore store = new MarketplaceStore();
        store.setStoreId(7);
        store.setStoreName("Store");
        Product marketplace = new Product();
        marketplace.setProId(7);
        marketplace.setProName("Market card");
        marketplace.setListingSource("MARKETPLACE");
        marketplace.setStore(store);
        Product withoutStore = new Product();
        withoutStore.setProId(8);
        withoutStore.setListingSource("MARKETPLACE");
        Product official = new Product();
        official.setProId(9);
        official.setListingSource("OFFICIAL");
        when(productRepository.findByListingSourceOrderByProIdDesc("MARKETPLACE"))
                .thenReturn(List.of(marketplace, withoutStore));
        assertEquals(2, adminDataService.getMarketplaceProducts(null).size());
        assertEquals(1, adminDataService.getMarketplaceProducts(7).size());
        when(productRepository.findByProId(7)).thenReturn(Optional.of(marketplace));
        assertEquals(7, adminDataService.getMarketplaceProduct(7).id());
        when(productRepository.findByProId(9)).thenReturn(Optional.of(official));
        assertThrows(ResponseStatusException.class, () -> adminDataService.getMarketplaceProduct(9));
    }

    @Test
    void smartRestockPredictions_coverNoSalesUrgentOutOfStockAndSufficientStock() {
        Product noSales = officialProduct(1, "No sales", 5, true);
        Product urgent = officialProduct(2, "Urgent", 1, true);
        Product out = officialProduct(3, "Out", 0, true);
        Product sufficient = officialProduct(4, "Sufficient", 100, true);
        Product inactive = officialProduct(5, "Inactive", 1, false);
        when(productRepository.findAllByOrderByProIdDesc()).thenReturn(List.of(noSales, urgent, out, sufficient, inactive));
        Order sales = new Order();
        org.example.model.OrderItem item = new org.example.model.OrderItem();
        item.setProductId(2);
        item.setQuantity(30);
        sales.setItems(List.of(item));
        Order manySales = new Order();
        org.example.model.OrderItem item2 = new org.example.model.OrderItem();
        item2.setProductId(3);
        item2.setQuantity(1);
        manySales.setItems(List.of(item2));
        Order enoughSales = new Order();
        org.example.model.OrderItem item3 = new org.example.model.OrderItem();
        item3.setProductId(4);
        item3.setQuantity(1);
        enoughSales.setItems(List.of(item3));
        when(orderRepository.findByCreatedAtBetween(any(), any())).thenReturn(List.of(sales, manySales, enoughSales));

        List<AdminData.RestockPredictionResponse> results = adminDataService.getSmartRestockPredictions();
        assertEquals(4, results.size());
        assertEquals("There is not enough information.", results.get(0).statusMessage());
        assertEquals("Restock immediately!", results.get(1).statusMessage());
        assertEquals("Out of stock!", results.get(2).statusMessage());
        assertEquals("Stock is still sufficient.", results.get(3).statusMessage());
    }

    @Test
    void orderStatusValidation_coversMissingAndAlreadyShippedBranches() {
        assertBadRequest(() -> adminDataService.updateOrderStatus(1, "UNKNOWN", null));
        when(orderRepository.findById(99)).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> adminDataService.updateOrderStatus(99, "DELIVERED", null));
        Order shipped = new Order();
        shipped.setOrderId(3);
        shipped.setStatus("SHIPPED");
        shipped.setItems(List.of());
        when(orderRepository.findById(3)).thenReturn(Optional.of(shipped));
        when(orderRepository.saveAndFlush(shipped)).thenReturn(shipped);
        when(productRepository.findAllByOrderByProIdDesc()).thenReturn(List.of());
        when(userRepository.findAll()).thenReturn(List.of());
        assertEquals("Shipped", adminDataService.updateOrderStatus(3, "SHIPPED", null).status());
        verify(notificationService, never()).createShippingNotification(any());
    }

    private AdminData.ProductRequest validRequest() {
        return new AdminData.ProductRequest("Card", "Pokemon", "Single", 1d, 2d, 3, null, null, null, null);
    }

    private Product officialProduct(int id, String name, int stock, boolean active) {
        Product product = new Product();
        product.setProId(id);
        product.setProName(name);
        product.setListingSource("OFFICIAL");
        product.setProQuantity(stock);
        product.setIsActive(active);
        return product;
    }

    private void assertBadRequest(org.junit.jupiter.api.function.Executable executable) {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, executable);
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }
}
