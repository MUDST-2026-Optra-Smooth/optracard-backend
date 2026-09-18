package org.example.repository;

import org.example.model.MarketplaceStore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface MarketplaceStoreRepository extends JpaRepository<MarketplaceStore, Integer> {
    Optional<MarketplaceStore> findByStoreSlug(String storeSlug);
    Optional<MarketplaceStore> findFirstBySellerUserIdOrderByStoreIdDesc(Integer sellerUserId);
    Optional<MarketplaceStore> findByStoreId(Integer storeId);
    List<MarketplaceStore> findAllBySellerUserId(Integer sellerUserId);
}
