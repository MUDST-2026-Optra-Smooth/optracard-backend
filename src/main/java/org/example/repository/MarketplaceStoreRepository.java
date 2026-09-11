package org.example.repository;

import org.example.model.MarketplaceStore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MarketplaceStoreRepository extends JpaRepository<MarketplaceStore, Integer> {
    Optional<MarketplaceStore> findByStoreSlug(String storeSlug);
}
