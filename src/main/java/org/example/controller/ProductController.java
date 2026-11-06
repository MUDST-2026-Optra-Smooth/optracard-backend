package org.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.dto.CatalogProductResponse;
import org.example.model.Product;
import org.example.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*")
@Tag(name = "Products", description = "Endpoints for browsing, searching, and creating products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
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
            summary = "Get product by ID",
            description = "Retrieve details of a specific catalog product by its ID."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product details retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<CatalogProductResponse> getProduct(@PathVariable Integer id) {
        return productService.getProductById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "Get product offers",
            description = "Retrieve all active and approved offers for a specific product item, sorted by price."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product offers retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @GetMapping("/{id}/offers")
    public ResponseEntity<List<CatalogProductResponse>> getProductOffers(@PathVariable Integer id) {
        return productService.getProductOffers(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
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
            @ApiResponse(
                    responseCode = "200",
                    description = "Matching products retrieved successfully"
            )
    })
    @GetMapping("/search")
    public ResponseEntity<List<CatalogProductResponse>> searchProducts(
            @Parameter(
                    description = "Keyword to search for in product name or card game name",
                    example = "Pokemon"
            )
            @RequestParam(name = "q", defaultValue = "") String q) {
        return ResponseEntity.ok(productService.searchProducts(q));
    }

    @GetMapping("/catalog")
    public ResponseEntity<org.example.dto.CatalogPageResponse> catalog(@RequestParam(defaultValue="") String q,@RequestParam(required=false) String type,@RequestParam(required=false) String game,@RequestParam(required=false) Double minPrice,@RequestParam(required=false) Double maxPrice,@RequestParam(required=false) String language,@RequestParam(required=false) String source,@RequestParam(required=false) Integer storeId,@RequestParam(defaultValue="newest") String sort,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="24") int pageSize) {
        if((minPrice!=null&&minPrice<0)||(maxPrice!=null&&maxPrice<0)||(minPrice!=null&&maxPrice!=null&&minPrice>maxPrice)||page<0||pageSize<1||pageSize>100||(source!=null&&!source.isBlank()&&!List.of("OFFICIAL","MARKETPLACE").contains(source.toUpperCase()))||!List.of("newest","price_asc","price_desc","name_asc").contains(sort)) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(productService.catalog(q,type,game,minPrice,maxPrice,language,source,storeId,sort,page,pageSize));
    }

    @GetMapping("/catalog/filter-options")
    public ResponseEntity<org.example.dto.CatalogFilterOptionsResponse> catalogFilterOptions() { return ResponseEntity.ok(productService.filterOptions()); }
}
