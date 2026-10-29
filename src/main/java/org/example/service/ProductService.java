package org.example.service;

import org.example.dto.CatalogProductResponse;
import org.example.dto.CatalogStoreResponse;
import org.example.dto.MarketplaceStoreProfileResponse;
import org.example.dto.CatalogPageResponse;
import org.example.dto.CatalogFilterOptionsResponse;
import org.example.model.MarketplaceStore;
import org.example.model.Product;
import org.example.repository.MarketplaceStoreRepository;
import org.example.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import static org.example.service.CatalogProductSpecification.publicProduct;

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

        return productRepository.findByIsActiveTrueOrderByProTypeAscProIdAsc()
                .stream()
                .filter(this::isPublicCatalogProduct)
                .map(product -> new ScoredProduct(product, searchScore(normalizedKeyword, product)))
                .filter(result -> result.score() != CatalogSearchMatcher.NO_MATCH)
                .sorted(Comparator.comparingInt(ScoredProduct::score)
                        .thenComparing(result -> result.product().getProId(), Comparator.reverseOrder()))
                .map(result -> toCatalogResponse(result.product()))
                .toList();
    }

    @Transactional(readOnly = true)
    public CatalogPageResponse catalog(String q,String type,String game,Double min,Double max,String language,String source,Integer storeId,String sort,int page,int pageSize) {
        Sort ordering=switch(sort){case "price_asc"->Sort.by("proPriceOfSell").ascending().and(Sort.by("proId").descending());case "price_desc"->Sort.by("proPriceOfSell").descending().and(Sort.by("proId").descending());case "name_asc"->Sort.by("proName").ascending().and(Sort.by("proId").descending());default->Sort.by("proId").descending();};
        var result=productRepository.findAll(publicProduct(q,type,game,min,max,language,source,storeId),PageRequest.of(page,pageSize,ordering));
        return new CatalogPageResponse(result.getContent().stream().map(this::toCatalogResponse).toList(),result.getNumber(),result.getSize(),result.getTotalElements(),result.getTotalPages(),sort);
    }

    @Transactional(readOnly = true)
    public CatalogFilterOptionsResponse filterOptions() {
        var types=productRepository.findPublicCatalogTypes().stream().filter(Objects::nonNull).sorted().toList();
        var games=productRepository.findPublicCatalogGames().stream().map(row -> new CatalogFilterOptionsResponse.GameOption((Integer) row[0], (String) row[1])).sorted(java.util.Comparator.comparing(CatalogFilterOptionsResponse.GameOption::name)).toList();
        var languages=productRepository.findPublicCatalogLanguages().stream().filter(Objects::nonNull).sorted().toList();
        var sources=productRepository.findPublicCatalogSources().stream().filter(Objects::nonNull).sorted().toList();
        var stores=productRepository.findPublicCatalogStores().stream().map(row -> new CatalogFilterOptionsResponse.StoreOption((Integer) row[0], (String) row[1])).sorted(java.util.Comparator.comparing(CatalogFilterOptionsResponse.StoreOption::name)).toList();
        var priceRange=productRepository.findPublicCatalogPriceRange();
        double min=priceRange[0] instanceof Number value ? value.doubleValue() : 0d;
        double max=priceRange[1] instanceof Number value ? value.doubleValue() : 0d;
        return new CatalogFilterOptionsResponse(types,games,languages,sources,stores,new CatalogFilterOptionsResponse.PriceRange(min,max));
    }

    private record ScoredProduct(Product product, int score) {}

    private int searchScore(String query, Product product) {
        String game = product.getCardGame() == null ? "Uncategorized" : product.getCardGame().getGameName();
        String store = "MARKETPLACE".equalsIgnoreCase(product.getListingSource())
                ? (product.getStore() == null ? "Marketplace seller" : product.getStore().getStoreName())
                : "Optracard Official Store";
        return CatalogSearchMatcher.score(query, product.getProName(), game, product.getProType(),
                product.getProDescription(), store);
    }

    public Product createProduct(Product product) {
        return productRepository.save(product);
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
