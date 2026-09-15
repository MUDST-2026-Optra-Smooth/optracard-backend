package org.example.controller;

import org.example.dto.CatalogProductResponse;
import org.example.model.Product;
import org.example.service.ProductService;
import org.example.repository.ProductRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*")
public class ProductController {

    private final ProductService productService;
    private final ProductRepository productRepository;

    public ProductController(ProductService productService, ProductRepository productRepository) {
        this.productService = productService;
        this.productRepository = productRepository;
    }

    @GetMapping
    public List<CatalogProductResponse> getAllProducts() {
        return productService.getAllProducts();
    }

    @GetMapping("/home")
    public List<CatalogProductResponse> getHomeProducts() {
        return productService.getAllProducts();
    }

    @PostMapping
    public ResponseEntity<Product> createProduct(@RequestBody Product product) {
        return ResponseEntity.ok(productService.createProduct(product));
    }

    @GetMapping("/search")
    public ResponseEntity<List<Product>> searchProducts(@RequestParam(name = "q", defaultValue = "") String q) {
        if (q.trim().isEmpty()) {
            return ResponseEntity.ok(productRepository.findAll());
        }
        List<Product> results = productRepository.searchByKeyword(q);
        return ResponseEntity.ok(results);
    }
}