package org.example.config;

import org.example.model.CardGame;
import org.example.model.MarketplaceStore;
import org.example.model.Order;
import org.example.model.Product;
import org.example.model.User;
import org.example.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LocalCatalogSeederTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CardGameRepository cardGameRepository;

    @Mock
    private MarketplaceStoreRepository marketplaceStoreRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuctionRepository auctionRepository;

    @InjectMocks
    private LocalCatalogSeeder seeder;

    @BeforeEach
    void setUp() {
        CardGame game = new CardGame();
        game.setGameId(1);
        game.setGameName("Pokemon");

        when(cardGameRepository.findByGameName(anyString())).thenReturn(Optional.of(game));
        when(cardGameRepository.findByGameNameIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(cardGameRepository.save(any(CardGame.class))).thenAnswer(inv -> inv.getArgument(0));

        User seller = new User();
        seller.setUaId(10);
        seller.setEmail("seller.demo@optracard.local");
        seller.setRole("SELLER");
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(seller));

        MarketplaceStore store = new MarketplaceStore();
        store.setStoreId(100);
        store.setStoreSlug("aaa-trading");
        store.setStoreStatus("APPROVED");
        when(marketplaceStoreRepository.findByStoreSlug(anyString())).thenReturn(Optional.of(store));
        when(marketplaceStoreRepository.save(any(MarketplaceStore.class))).thenAnswer(inv -> inv.getArgument(0));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("Should skip products that already exist and mark pending orders as PAID")
    void run_whenProductsExist_skipsSavingExisting() {
        when(productRepository.existsByProNameAndListingSource(anyString(), anyString())).thenReturn(true);
        when(productRepository.existsByProNameAndListingSourceAndStore_StoreId(anyString(), anyString(), anyInt())).thenReturn(true);

        Order pendingOrder = new Order();
        pendingOrder.setOrderId(1);
        pendingOrder.setPaymentStatus("PENDING");

        when(orderRepository.findAll()).thenReturn(List.of(pendingOrder));
        when(productRepository.findAll()).thenReturn(Collections.emptyList());

        seeder.run(null);

        assertEquals("PAID", pendingOrder.getPaymentStatus());
        verify(orderRepository).saveAll(List.of(pendingOrder));
        verify(productRepository, never()).save(any(Product.class));
        verifyNoInteractions(auctionRepository);
    }

    @Test
    @DisplayName("Should create local demo sellers and approved marketplace stores when the database is empty")
    void run_whenLocalDatabaseHasNoDemoUsers_createsApprovedMarketplaceStores() {
        Map<String, User> persistedUsers = new HashMap<>();
        when(userRepository.findByEmail(anyString())).thenAnswer(invocation ->
                Optional.ofNullable(persistedUsers.get(invocation.getArgument(0))));
        AtomicInteger userId = new AtomicInteger(10);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User seller = invocation.getArgument(0);
            if (seller.getUaId() == null) {
                seller.setUaId(userId.getAndIncrement());
            }
            persistedUsers.put(seller.getEmail(), seller);
            return seller;
        });
        when(userRepository.findAll()).thenReturn(Collections.emptyList());
        when(marketplaceStoreRepository.findByStoreSlug(anyString())).thenReturn(Optional.empty());
        AtomicInteger storeId = new AtomicInteger(100);
        when(marketplaceStoreRepository.save(any(MarketplaceStore.class))).thenAnswer(invocation -> {
            MarketplaceStore store = invocation.getArgument(0);
            if (store.getStoreId() == null) {
                store.setStoreId(storeId.getAndIncrement());
            }
            return store;
        });
        when(productRepository.existsByProNameAndListingSource(anyString(), anyString())).thenReturn(true);
        when(productRepository.existsByProNameAndListingSourceAndStore_StoreId(anyString(), anyString(), anyInt())).thenReturn(true);
        when(productRepository.findAll()).thenReturn(Collections.emptyList());
        when(productRepository.findByListingSourceOrderByProIdDesc(anyString())).thenReturn(Collections.emptyList());
        when(orderRepository.findAll()).thenReturn(Collections.emptyList());

        seeder.run(null);

        var sellerCaptor = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(4)).save(sellerCaptor.capture());
        assertEquals(List.of("SELLER", "SELLER", "SELLER", "SELLER"),
                sellerCaptor.getAllValues().stream().map(User::getRole).toList());

        var storeCaptor = org.mockito.ArgumentCaptor.forClass(MarketplaceStore.class);
        verify(marketplaceStoreRepository, times(4)).save(storeCaptor.capture());
        assertEquals(List.of("APPROVED", "APPROVED", "APPROVED", "APPROVED"),
                storeCaptor.getAllValues().stream().map(MarketplaceStore::getStoreStatus).toList());
        verifyNoInteractions(auctionRepository);
    }

    @Test
    @DisplayName("Seeds the complete local catalog when the database is empty")
    void run_whenDatabaseIsEmpty_seedsGamesStoresAndProducts() {
        Map<String, CardGame> games = new HashMap<>();
        AtomicInteger gameId = new AtomicInteger(1);
        when(cardGameRepository.findByGameName(anyString())).thenAnswer(invocation ->
                Optional.ofNullable(games.get(invocation.getArgument(0))));
        when(cardGameRepository.save(any(CardGame.class))).thenAnswer(invocation -> {
            CardGame game = invocation.getArgument(0);
            game.setGameId(gameId.getAndIncrement());
            games.put(game.getGameName(), game);
            return game;
        });

        Map<String, User> users = new HashMap<>();
        AtomicInteger userId = new AtomicInteger(1);
        when(userRepository.findByEmail(anyString())).thenAnswer(invocation ->
                Optional.ofNullable(users.get(invocation.getArgument(0))));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            if (user.getUaId() == null) user.setUaId(userId.getAndIncrement());
            users.put(user.getEmail(), user);
            return user;
        });

        Map<String, MarketplaceStore> stores = new HashMap<>();
        AtomicInteger storeId = new AtomicInteger(100);
        when(marketplaceStoreRepository.findByStoreSlug(anyString())).thenAnswer(invocation ->
                Optional.ofNullable(stores.get(invocation.getArgument(0))));
        when(marketplaceStoreRepository.save(any(MarketplaceStore.class))).thenAnswer(invocation -> {
            MarketplaceStore store = invocation.getArgument(0);
            if (store.getStoreId() == null) store.setStoreId(storeId.getAndIncrement());
            stores.put(store.getStoreSlug(), store);
            return store;
        });

        when(productRepository.existsByProNameAndListingSource(anyString(), anyString())).thenReturn(false);
        when(productRepository.existsByProNameAndListingSourceAndStore_StoreId(anyString(), anyString(), anyInt())).thenReturn(false);
        when(productRepository.findAll()).thenReturn(Collections.emptyList());
        when(productRepository.findByListingSourceOrderByProIdDesc(anyString())).thenReturn(Collections.emptyList());
        when(orderRepository.findAll()).thenReturn(Collections.emptyList());

        seeder.run(null);

        verify(cardGameRepository, times(4)).save(any(CardGame.class));
        verify(userRepository, times(4)).save(any(User.class));
        verify(marketplaceStoreRepository, times(4)).save(any(MarketplaceStore.class));
        verify(productRepository, atLeast(20)).save(any(Product.class));
    }
}
