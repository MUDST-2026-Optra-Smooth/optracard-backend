package org.example.controller;

import org.example.dto.CatalogProductResponse;
import org.example.dto.CatalogStoreResponse;
import org.example.model.Product;
import org.example.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock
    private ProductService productService;

    private ProductController productController;

    @BeforeEach
    void setUp() {
        productController = new ProductController(productService);
    }

    @Test
    void getAllProducts_returnsCatalogProductList() {
        CatalogProductResponse sample = new CatalogProductResponse(
                1,
                "SKU-001",
                "Charizard",
                "Single Card",
                "Pokemon",
                1500.0,
                5,
                "http://example.com/charizard.jpg",
                "Rare holographic card",
                "OFFICIAL",
                new CatalogStoreResponse(null, "Optracard Official Store", "optracard-official")
        );
        when(productService.getAllProducts()).thenReturn(List.of(sample));

        ResponseEntity<List<CatalogProductResponse>> response = productController.getAllProducts();

        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("Charizard", response.getBody().get(0).name());
        verify(productService).getAllProducts();
    }

    @Test
    void createProduct_savesAndReturnsProduct() {
        Product input = new Product();
        input.setProName("Pikachu");
        input.setProPriceOfSell(250.0);

        when(productService.createProduct(input)).thenReturn(input);

        ResponseEntity<Product> response = productController.createProduct(input);

        assertNotNull(response.getBody());
        assertEquals("Pikachu", response.getBody().getProName());
        verify(productService).createProduct(input);
    }

    @Test
    void searchProducts_delegatesToProductService() {
        Product found = new Product();
        found.setProName("Pikachu");

        when(productService.searchProducts("Pikachu")).thenReturn(List.of(found));

        ResponseEntity<List<Product>> response = productController.searchProducts("Pikachu");

        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("Pikachu", response.getBody().get(0).getProName());
        verify(productService).searchProducts("Pikachu");
    }
}
