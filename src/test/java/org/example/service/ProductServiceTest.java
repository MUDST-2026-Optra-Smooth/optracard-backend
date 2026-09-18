package org.example.service;

import org.example.model.Product;
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

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productRepository);
    }

    @Test
    void searchProducts_whenEmptyKeyword_returnsAllProducts() {
        Product p = new Product();
        p.setProName("Test Card");
        when(productRepository.findAll()).thenReturn(List.of(p));

        List<Product> results = productService.searchProducts("");

        assertNotNull(results);
        assertEquals(1, results.size());
        verify(productRepository).findAll();
    }

    @Test
    void searchProducts_whenKeywordProvided_searchesByKeyword() {
        Product p = new Product();
        p.setProName("Charizard");
        when(productRepository.searchByKeyword("Charizard")).thenReturn(List.of(p));

        List<Product> results = productService.searchProducts(" Charizard ");

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("Charizard", results.get(0).getProName());
        verify(productRepository).searchByKeyword("Charizard");
    }
}
