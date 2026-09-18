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
import java.util.Optional;

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

    private CatalogProductResponse createSampleProduct(Integer id, String name, Double price) {
        return new CatalogProductResponse(
                id,
                name,
                "Single Card",
                "Pokemon",
                price,
                5,
                "http://example.com/" + name.toLowerCase() + ".jpg",
                "Rare holographic card",
                "Base Set",
                "English",
                "OFFICIAL",
                new CatalogStoreResponse(null, "Optracard Official Store", "optracard-official")
        );
    }

    @Test
    void getAllProducts_returnsCatalogProductList() {
        CatalogProductResponse sample = createSampleProduct(1, "Charizard", 1500.0);
        when(productService.getAllProducts()).thenReturn(List.of(sample));

        ResponseEntity<List<CatalogProductResponse>> response = productController.getAllProducts();

        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("Charizard", response.getBody().get(0).name());
        verify(productService).getAllProducts();
    }

    @Test
    void getProduct_whenFound_returnsProduct() {
        CatalogProductResponse sample = createSampleProduct(1, "Charizard", 1500.0);
        when(productService.getProductById(1)).thenReturn(Optional.of(sample));

        ResponseEntity<CatalogProductResponse> response = productController.getProduct(1);

        assertNotNull(response.getBody());
        assertEquals("Charizard", response.getBody().name());
        verify(productService).getProductById(1);
    }

    @Test
    void getProduct_whenNotFound_returns404() {
        when(productService.getProductById(99)).thenReturn(Optional.empty());

        ResponseEntity<CatalogProductResponse> response = productController.getProduct(99);

        assertEquals(404, response.getStatusCode().value());
        verify(productService).getProductById(99);
    }

    @Test
    void getProductOffers_whenFound_returnsOffersList() {
        CatalogProductResponse offer = createSampleProduct(2, "Charizard", 1400.0);
        when(productService.getProductOffers(1)).thenReturn(Optional.of(List.of(offer)));

        ResponseEntity<List<CatalogProductResponse>> response = productController.getProductOffers(1);

        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(1400.0, response.getBody().get(0).price());
        verify(productService).getProductOffers(1);
    }

    @Test
    void getProductOffers_whenNotFound_returns404() {
        when(productService.getProductOffers(99)).thenReturn(Optional.empty());

        ResponseEntity<List<CatalogProductResponse>> response = productController.getProductOffers(99);

        assertEquals(404, response.getStatusCode().value());
        verify(productService).getProductOffers(99);
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
        CatalogProductResponse sample = createSampleProduct(2, "Pikachu", 250.0);

        when(productService.searchProducts("Pikachu")).thenReturn(List.of(sample));

        ResponseEntity<List<CatalogProductResponse>> response = productController.searchProducts("Pikachu");

        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("Pikachu", response.getBody().get(0).name());
        verify(productService).searchProducts("Pikachu");
    }
}
