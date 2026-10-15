package org.example.service;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SellerServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private MarketplaceStoreRepository storeRepository;
    @Mock private UserRepository userRepository;
    @Mock private CardGameRepository cardGameRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private OrderNotificationService notificationService;
    @Mock private ProductDeletionService productDeletionService;

    private SellerService sellerService;

    @BeforeEach
    void setUp() {
        sellerService = new SellerService(productRepository, storeRepository, userRepository, cardGameRepository, orderRepository,
                notificationService, productDeletionService);
    }

    @Test
    void createProduct_allowsAnOfficialListingToBeUsedAsATemplate() {
        User seller = new User();
        seller.setUaId(7);
        MarketplaceStore sellerStore = new MarketplaceStore();
        sellerStore.setStoreId(9);
        sellerStore.setStoreName("Independent TCG");
        sellerStore.setStoreStatus("APPROVED");

        CardGame onePiece = new CardGame();
        onePiece.setGameId(3);
        onePiece.setGameName("One Piece");
        Product officialProduct = new Product();
        officialProduct.setProId(21);
        officialProduct.setProName("One Piece OP-10 Royal Blood Booster");
        officialProduct.setProType("Booster");
        officialProduct.setGameId(3);
        officialProduct.setCardGame(onePiece);
        officialProduct.setProductSet("Royal Blood [OP-10]");
        officialProduct.setLanguage("Japanese");
        officialProduct.setProImageUrl("https://example.com/op10.jpg");
        officialProduct.setListingSource("OFFICIAL");
        officialProduct.setIsActive(true);

        SellerProductRequest request = new SellerProductRequest(
                "One Piece OP-10 Royal Blood Booster", "One Piece", "Booster",
                120.0, 145.0, 20, null, "", "", "Independent shop price", 21
        );

        when(userRepository.findByEmail("seller@example.test")).thenReturn(Optional.of(seller));
        when(storeRepository.findFirstBySellerUserIdOrderByStoreIdDesc(7)).thenReturn(Optional.of(sellerStore));
        when(productRepository.findByProId(21)).thenReturn(Optional.of(officialProduct));
        when(productRepository.saveAndFlush(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SellerProductResponse result = sellerService.createProduct("seller@example.test", request);

        ArgumentCaptor<Product> savedProduct = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).saveAndFlush(savedProduct.capture());
        Product listing = savedProduct.getValue();
        assertEquals("MARKETPLACE", listing.getListingSource());
        assertEquals("PENDING", listing.getApprovalStatus());
        assertEquals(21, listing.getTemplateProductId());
        assertEquals("One Piece OP-10 Royal Blood Booster", listing.getProName());
        assertEquals(145.0, listing.getProPriceOfSell());
        assertEquals("Independent TCG", result.storeName());
    }

    @Test
    void shippingAnOrderRequiresTrackingAndNotifiesTheBuyer() {
        User seller = new User();
        seller.setUaId(7);
        MarketplaceStore sellerStore = new MarketplaceStore();
        sellerStore.setStoreId(9);
        sellerStore.setStoreStatus("APPROVED");

        Order order = new Order();
        order.setOrderId(44);
        order.setUserId(30);
        order.setStoreId(9);
        order.setStatus("Processing");
        order.setTotalPrice(100d);

        when(userRepository.findByEmail("seller@example.test")).thenReturn(Optional.of(seller));
        when(storeRepository.findFirstBySellerUserIdOrderByStoreIdDesc(7)).thenReturn(Optional.of(sellerStore));
        when(orderRepository.findByOrderIdAndStoreId(44, 9)).thenReturn(Optional.of(order));
        when(orderRepository.saveAndFlush(order)).thenReturn(order);

        assertThrows(ResponseStatusException.class,
                () -> sellerService.updateOrderStatus("seller@example.test", 44, "SHIPPED", " "));

        var result = sellerService.updateOrderStatus("seller@example.test", 44, "SHIPPED", " TH123456789 ");

        assertEquals("Shipped", result.status());
        assertEquals("TH123456789", result.trackingNumber());
        verify(notificationService).createShippingNotification(order);
    }

    @Test
    void updateProductActive_changesOnlyTheSellerOwnedListingVisibility() {
        User seller = new User();
        seller.setUaId(7);
        MarketplaceStore sellerStore = new MarketplaceStore();
        sellerStore.setStoreId(9);
        sellerStore.setStoreName("Independent TCG");
        sellerStore.setStoreStatus("APPROVED");
        Product listing = new Product();
        listing.setProId(12);
        listing.setStore(sellerStore);
        listing.setIsActive(true);

        when(userRepository.findByEmail("seller@example.test")).thenReturn(Optional.of(seller));
        when(storeRepository.findFirstBySellerUserIdOrderByStoreIdDesc(7)).thenReturn(Optional.of(sellerStore));
        when(productRepository.findByProIdAndStore_StoreId(12, 9)).thenReturn(Optional.of(listing));
        when(productRepository.saveAndFlush(listing)).thenReturn(listing);

        SellerProductResponse result = sellerService.updateProductActive("seller@example.test", 12, false);

        assertEquals(false, result.active());
        verify(productRepository).saveAndFlush(listing);
    }

    @Test
    void deleteProduct_delegatesPermanentRemovalForTheSellersOwnListing() {
        User seller = new User();
        seller.setUaId(7);
        MarketplaceStore sellerStore = new MarketplaceStore();
        sellerStore.setStoreId(9);
        sellerStore.setStoreStatus("APPROVED");
        Product listing = new Product();
        listing.setProId(12);
        listing.setStore(sellerStore);

        when(userRepository.findByEmail("seller@example.test")).thenReturn(Optional.of(seller));
        when(storeRepository.findFirstBySellerUserIdOrderByStoreIdDesc(7)).thenReturn(Optional.of(sellerStore));
        when(productRepository.findByProIdAndStore_StoreId(12, 9)).thenReturn(Optional.of(listing));

        sellerService.deleteProduct("seller@example.test", 12);

        verify(productDeletionService).permanentlyDelete(listing);
    }
}
