package org.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.dto.CatalogProductResponse;
import org.example.model.Product;
import org.example.repository.ProductRepository;
import org.example.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*")
@Tag(name = "Products", description = "Endpoints for browsing, searching, and creating products")
public class ProductController {

    private final ProductService productService;
    private final ProductRepository productRepository;

    public ProductController(ProductService productService) {
        this(productService, null);
    }

    @Autowired
    public ProductController(ProductService productService, @Autowired(required = false) ProductRepository productRepository) {
        this.productService = productService;
        this.productRepository = productRepository;
    }

    @Operation(
            summary = "Get all catalog products",
            description = "Retrieve all active and approved products for storefront catalog and home display."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of catalog products retrieved successfully")
    })
    @GetMapping({"", "/home"})
    public ResponseEntity<List<CatalogProductResponse>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @Operation(
            summary = "Create a new product",
            description = "Add a new product entry to the catalog."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product successfully created")
    })
    @PostMapping
    public ResponseEntity<Product> createProduct(@RequestBody Product product) {
        return ResponseEntity.ok(productService.createProduct(product));
    }

    @Operation(
            summary = "Search products",
            description = "Search products by keyword matching product name or card game name. If no keyword is given, returns all products."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Matching products retrieved successfully")
    })
    @GetMapping("/search")
    public ResponseEntity<List<Product>> searchProducts(
            @Parameter(description = "Keyword to search for in product name or card game name", example = "Pokemon")
            @RequestParam(name = "q", defaultValue = "") String q) {
        return ResponseEntity.ok(productService.searchProducts(q));
    }
}
