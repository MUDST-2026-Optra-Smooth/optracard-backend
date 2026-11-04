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
import java.util.List;
import java.util.Optional;

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
    }
}
