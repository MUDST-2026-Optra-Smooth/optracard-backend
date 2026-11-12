package org.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.dto.CatalogFilterOptionsResponse;
import org.example.dto.CatalogPageResponse;
import org.example.dto.CatalogProductResponse;
import org.example.model.Product;
import org.example.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*")
@Tag(name = "Products", description = "Public endpoints for browsing, faceted search, filtering, and official product creation")
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
    public ResponseEntity<CatalogProductResponse> getProduct(
            @Parameter(description = "Product ID", example = "1") @PathVariable Integer id) {
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
    public ResponseEntity<List<CatalogProductResponse>> getProductOffers(
            @Parameter(description = "Product ID", example = "1") @PathVariable Integer id) {
        return productService.getProductOffers(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "Create a new product",
            description = "Add a new product entry to the catalog. Requires administrative privileges.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product successfully created"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN authority")
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
    public ResponseEntity<List<CatalogProductResponse>> searchProducts(
            @Parameter(
                    description = "Keyword to search for in product name or card game name",
                    example = "Pokemon"
            )
            @RequestParam(name = "q", defaultValue = "") String q) {
        return ResponseEntity.ok(productService.searchProducts(q));
    }

    @Operation(
            summary = "Browse and filter catalog products",
            description = "Retrieve paginated and filtered catalog products with multi-facet filters (query, game, type, price range, language, source, and store)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Paginated catalog page retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid filter bounds or unrecognized parameter value")
    })
    @GetMapping("/catalog")
    public ResponseEntity<CatalogPageResponse> catalog(
            @Parameter(description = "Search keyword query") @RequestParam(defaultValue = "") String q,
            @Parameter(description = "Product type (Single, Booster, Booster Box, Accessories)") @RequestParam(required = false) String type,
            @Parameter(description = "Card game category") @RequestParam(required = false) String game,
            @Parameter(description = "Minimum price bound") @RequestParam(required = false) Double minPrice,
            @Parameter(description = "Maximum price bound") @RequestParam(required = false) Double maxPrice,
            @Parameter(description = "Language (English, Japanese, Thai)") @RequestParam(required = false) String language,
            @Parameter(description = "Listing source: OFFICIAL or MARKETPLACE") @RequestParam(required = false) String source,
            @Parameter(description = "Store ID") @RequestParam(required = false) Integer storeId,
            @Parameter(description = "Sort criteria: newest, price_asc, price_desc, name_asc") @RequestParam(defaultValue = "newest") String sort,
            @Parameter(description = "Page index (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (1-100)") @RequestParam(defaultValue = "24") int pageSize) {
        if ((minPrice != null && minPrice < 0) || (maxPrice != null && maxPrice < 0) || (minPrice != null && maxPrice != null && minPrice > maxPrice) || page < 0 || pageSize < 1 || pageSize > 100 || (source != null && !source.isBlank() && !List.of("OFFICIAL", "MARKETPLACE").contains(source.toUpperCase())) || !List.of("newest", "price_asc", "price_desc", "name_asc").contains(sort))
            return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(productService.catalog(q, type, game, minPrice, maxPrice, language, source, storeId, sort, page, pageSize));
    }

    @Operation(
            summary = "Get catalog filter facet options",
            description = "Retrieve available facet filter options including card games, product types, and languages present in active listings."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Filter options retrieved successfully")
    })
    @GetMapping("/catalog/filter-options")
    public ResponseEntity<CatalogFilterOptionsResponse> catalogFilterOptions() {
        return ResponseEntity.ok(productService.filterOptions());
    }
}
