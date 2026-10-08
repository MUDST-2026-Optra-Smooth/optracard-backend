package org.example.service;

import org.example.model.Product;
import org.example.repository.CardGameRepository;
import org.example.repository.MarketplaceStoreRepository;
import org.example.repository.OrderRepository;
import org.example.repository.ProductRepository;
import org.example.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDataServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private MarketplaceStoreRepository storeRepository;
    @Mock private CardGameRepository cardGameRepository;
    @Mock private UserRepository userRepository;
    @Mock private OrderNotificationService notificationService;
    @Mock private ProductDeletionService productDeletionService;

    private AdminDataService adminDataService;

    @BeforeEach
    void setUp() {
        adminDataService = new AdminDataService(productRepository, orderRepository, storeRepository,
                cardGameRepository, userRepository, notificationService, productDeletionService);
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
}
