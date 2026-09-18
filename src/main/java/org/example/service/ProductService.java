package org.example.service;

import org.example.dto.CatalogProductResponse;
import org.example.dto.CatalogStoreResponse;
import org.example.dto.MarketplaceStoreProfileResponse;
import org.example.model.MarketplaceStore;
import org.example.model.Product;
import org.example.repository.MarketplaceStoreRepository;
import org.example.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final MarketplaceStoreRepository marketplaceStoreRepository;

    public ProductService(ProductRepository productRepository,
                          MarketplaceStoreRepository marketplaceStoreRepository) {
        this.productRepository = productRepository;
        this.marketplaceStoreRepository = marketplaceStoreRepository;
    }

    /**
     * Public catalog used by the storefront. The source field lets the UI
     * distinguish Optracard stock from products listed by marketplace sellers.
     */
    @Transactional(readOnly = true)
    public List<CatalogProductResponse> getAllProducts() {
        return productRepository.findByIsActiveTrueOrderByProTypeAscProIdAsc()
                .stream()
                .filter(this::isPublicCatalogProduct)
                .map(this::toCatalogResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<CatalogProductResponse> getProductById(Integer productId) {
        return productRepository.findByProId(productId)
                .filter(this::isPublicCatalogProduct)
                .map(this::toCatalogResponse);
    }

    /** Returns public listings of the same catalog item, sorted by price. */
    @Transactional(readOnly = true)
    public Optional<List<CatalogProductResponse>> getProductOffers(Integer productId) {
        return productRepository.findByProId(productId)
                .filter(this::isPublicCatalogProduct)
                .map(product -> productRepository
                        .findByProNameIgnoreCaseAndProTypeAndGameIdAndIsActiveTrueOrderByProPriceOfSellAscProIdAsc(
                                product.getProName(), product.getProType(), product.getGameId())
                        .stream()
                        .filter(this::isPublicCatalogProduct)
                        .filter(offer -> sameProductDetails(product, offer))
                        .map(this::toCatalogResponse)
                        .toList());
    }

    /** Public profile and live listings for an approved marketplace store. */
    @Transactional(readOnly = true)
    public Optional<MarketplaceStoreProfileResponse> getMarketplaceStoreProfile(Integer storeId) {
        return marketplaceStoreRepository.findByStoreId(storeId)
                .filter(store -> "APPROVED".equalsIgnoreCase(store.getStoreStatus()))
                .map(store -> {
                    List<CatalogProductResponse> products = productRepository
                            .findByStore_StoreIdOrderByProIdDesc(store.getStoreId())
                            .stream()
                            .filter(this::isPublicCatalogProduct)
                            .map(this::toCatalogResponse)
                            .toList();
                    return new MarketplaceStoreProfileResponse(
                            store.getStoreId(),
                            store.getStoreName(),
                            store.getStoreSlug(),
                            store.getStoreDescription(),
                            displayLocation(store),
                            store.getStoreProfileImage(),
                            products
                    );
                });
    }

    @Transactional(readOnly = true)
    public List<CatalogProductResponse> searchProducts(String keyword) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        if (normalizedKeyword.isEmpty()) {
            return getAllProducts();
        }

        return productRepository.searchByKeyword(normalizedKeyword)
                .stream()
                .filter(this::isPublicCatalogProduct)
                .map(this::toCatalogResponse)
                .toList();
    }

    public Product createProduct(Product product) {
        return productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public List<Product> searchProducts(String q) {
        if (q == null || q.trim().isEmpty()) {
            return productRepository.findAll();
        }
        return productRepository.searchByKeyword(q.trim());
    }

    private CatalogProductResponse toCatalogResponse(Product product) {
        String source = product.getListingSource() == null ? "OFFICIAL" : product.getListingSource();
        CatalogStoreResponse store = "MARKETPLACE".equalsIgnoreCase(source)
                ? toStoreResponse(product.getStore())
                : new CatalogStoreResponse(null, "Optracard Official Store", "optracard-official");

        return new CatalogProductResponse(
                product.getProId(),
                product.getProName(),
                product.getProType(),
                product.getCardGame() == null ? "Uncategorized" : product.getCardGame().getGameName(),
                product.getProPriceOfSell(),
                product.getProQuantity(),
                product.getProImageUrl(),
                product.getProDescription(),
                product.getProductSet(),
                product.getLanguage(),
                source.toUpperCase(),
                store
        );
    }

    private boolean isPublicCatalogProduct(Product product) {
        return Boolean.TRUE.equals(product.getIsActive())
                && (!"MARKETPLACE".equalsIgnoreCase(product.getListingSource())
                || ("APPROVED".equalsIgnoreCase(product.getApprovalStatus())
                && product.getStore() != null && "APPROVED".equalsIgnoreCase(product.getStore().getStoreStatus())));
    }

    private boolean sameProductDetails(Product first, Product second) {
        return Objects.equals(normalize(first.getProductSet()), normalize(second.getProductSet()))
                && Objects.equals(normalize(first.getLanguage()), normalize(second.getLanguage()));
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim().toLowerCase();
    }

    private CatalogStoreResponse toStoreResponse(MarketplaceStore store) {
        if (store == null) {
            return new CatalogStoreResponse(null, "Marketplace seller", null);
        }
        return new CatalogStoreResponse(store.getStoreId(), store.getStoreName(), store.getStoreSlug());
    }

    private String displayLocation(MarketplaceStore store) {
        return Stream.of(store.getDistrict(), store.getProvince())
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.joining(", "));
    }
}
