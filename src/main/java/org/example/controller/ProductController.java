package org.example.controller;

import org.example.dto.CatalogProductResponse;
import org.example.model.Product;
import org.example.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public List<CatalogProductResponse> getAllProducts() {
        return productService.getAllProducts();
    }

    @GetMapping("/home")
    public List<CatalogProductResponse> getHomeProducts() {
        return productService.getAllProducts();
    }

    @GetMapping("/{id}")
    public ResponseEntity<CatalogProductResponse> getProduct(@PathVariable Integer id) {
        return productService.getProductById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/offers")
    public ResponseEntity<List<CatalogProductResponse>> getProductOffers(@PathVariable Integer id) {
        return productService.getProductOffers(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Product> createProduct(@RequestBody Product product) {
        return ResponseEntity.ok(productService.createProduct(product));
    }

    @GetMapping("/search")
    public ResponseEntity<List<CatalogProductResponse>> searchProducts(@RequestParam(name = "q", defaultValue = "") String q) {
        return ResponseEntity.ok(productService.searchProducts(q));
    }
}
