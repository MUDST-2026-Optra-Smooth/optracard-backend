package org.example.service;

import org.example.dto.CatalogProductResponse;
import org.example.model.Product;
import org.example.repository.MarketplaceStoreRepository;
import org.example.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private MarketplaceStoreRepository marketplaceStoreRepository;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productRepository, marketplaceStoreRepository);
    }

    @Test
    void searchProducts_whenEmptyKeyword_returnsAllProducts() {
        Product p = new Product();
        p.setProId(1);
        p.setProName("Test Card");
        p.setIsActive(true);
        p.setListingSource("OFFICIAL");
        when(productRepository.findByIsActiveTrueOrderByProTypeAscProIdAsc()).thenReturn(List.of(p));

        List<CatalogProductResponse> results = productService.searchProducts("");

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("Test Card", results.get(0).name());
        verify(productRepository).findByIsActiveTrueOrderByProTypeAscProIdAsc();
    }

    @Test
    void searchProducts_whenKeywordProvided_searchesByKeyword() {
        Product p = new Product();
        p.setProId(2);
        p.setProName("Charizard");
        p.setIsActive(true);
        p.setListingSource("OFFICIAL");
        when(productRepository.searchByKeyword("Charizard")).thenReturn(List.of(p));

        List<CatalogProductResponse> results = productService.searchProducts(" Charizard ");

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("Charizard", results.get(0).name());
        verify(productRepository).searchByKeyword("Charizard");
    }
}
