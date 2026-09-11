package org.example.service;

import org.example.dto.CatalogProductResponse;
import org.example.dto.CatalogStoreResponse;
import org.example.model.MarketplaceStore;
import org.example.model.Product;
import org.example.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductService {
    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * Public catalog used by the storefront. The source field lets the UI
     * distinguish Optracard stock from products listed by marketplace sellers.
     */
    @Transactional(readOnly = true)
    public List<CatalogProductResponse> getAllProducts() {
        return productRepository.findByIsActiveTrueOrderByProTypeAscProIdAsc()
                .stream()
                .map(this::toCatalogResponse)
                .toList();
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
                product.getProSku(),
                product.getProName(),
                product.getProType(),
                product.getCardGame() == null ? "Uncategorized" : product.getCardGame().getGameName(),
                product.getProPriceOfSell(),
                product.getProQuantity(),
                product.getProImageUrl(),
                product.getProDescription(),
                source.toUpperCase(),
                store
        );
    }

    private CatalogStoreResponse toStoreResponse(MarketplaceStore store) {
        if (store == null) {
            return new CatalogStoreResponse(null, "Marketplace seller", null);
        }
        return new CatalogStoreResponse(store.getStoreId(), store.getStoreName(), store.getStoreSlug());
    }
}
