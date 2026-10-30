package org.example.repository;

import org.example.model.Product;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer>, JpaSpecificationExecutor<Product> {

    @Override
    @EntityGraph(attributePaths = {"cardGame", "store"})
    Page<Product> findAll(Specification<Product> specification, Pageable pageable);

    @EntityGraph(attributePaths = {"cardGame", "store"})
    List<Product> findByIsActiveTrueOrderByProTypeAscProIdAsc();

    @EntityGraph(attributePaths = {"cardGame", "store"})
    Optional<Product> findByProId(Integer proId);

    @EntityGraph(attributePaths = {"cardGame", "store"})
    List<Product> findByStore_StoreIdOrderByProIdDesc(Integer storeId);

    @EntityGraph(attributePaths = {"cardGame", "store"})
    Optional<Product> findByProIdAndStore_StoreId(Integer proId, Integer storeId);

    @EntityGraph(attributePaths = {"cardGame", "store"})
    List<Product> findByListingSourceOrderByProIdDesc(String listingSource);

    @EntityGraph(attributePaths = {"cardGame", "store"})
    List<Product> findAllByOrderByProIdDesc();

    List<Product> findByGameId(Integer gameId);

    boolean existsByProNameAndListingSource(String proName, String listingSource);

    boolean existsByProNameAndListingSourceAndStore_StoreId(String proName, String listingSource, Integer storeId);

    boolean existsByProNameIgnoreCaseAndProTypeAndGameIdAndListingSource(
            String proName,
            String proType,
            Integer gameId,
            String listingSource
    );

    @EntityGraph(attributePaths = {"cardGame", "store"})
    List<Product> findByProNameIgnoreCaseAndProTypeAndGameIdAndIsActiveTrueOrderByProPriceOfSellAscProIdAsc(
            String proName,
            String proType,
            Integer gameId
    );

    @Query("SELECT p FROM Product p LEFT JOIN p.cardGame g WHERE " +
            "LOWER(p.proName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(g.gameName) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Product> searchByKeyword(@Param("keyword") String keyword);

    @Query("SELECT DISTINCT p.proType FROM Product p LEFT JOIN p.store s WHERE p.isActive = true AND p.proType IS NOT NULL AND (p.listingSource IS NULL OR UPPER(p.listingSource) <> 'MARKETPLACE' OR (UPPER(p.approvalStatus) = 'APPROVED' AND UPPER(s.storeStatus) = 'APPROVED'))")
    List<String> findPublicCatalogTypes();

    @Query("SELECT DISTINCT p.gameId, g.gameName FROM Product p LEFT JOIN p.cardGame g LEFT JOIN p.store s WHERE p.isActive = true AND p.gameId IS NOT NULL AND g.gameName IS NOT NULL AND (p.listingSource IS NULL OR UPPER(p.listingSource) <> 'MARKETPLACE' OR (UPPER(p.approvalStatus) = 'APPROVED' AND UPPER(s.storeStatus) = 'APPROVED'))")
    List<Object[]> findPublicCatalogGames();

    @Query("SELECT DISTINCT p.language FROM Product p LEFT JOIN p.store s WHERE p.isActive = true AND p.language IS NOT NULL AND p.language <> '' AND (p.listingSource IS NULL OR UPPER(p.listingSource) <> 'MARKETPLACE' OR (UPPER(p.approvalStatus) = 'APPROVED' AND UPPER(s.storeStatus) = 'APPROVED'))")
    List<String> findPublicCatalogLanguages();

    @Query("SELECT DISTINCT COALESCE(UPPER(p.listingSource), 'OFFICIAL') FROM Product p LEFT JOIN p.store s WHERE p.isActive = true AND (p.listingSource IS NULL OR UPPER(p.listingSource) <> 'MARKETPLACE' OR (UPPER(p.approvalStatus) = 'APPROVED' AND UPPER(s.storeStatus) = 'APPROVED'))")
    List<String> findPublicCatalogSources();

    @Query("SELECT DISTINCT s.storeId, s.storeName FROM Product p JOIN p.store s WHERE p.isActive = true AND UPPER(p.listingSource) = 'MARKETPLACE' AND UPPER(p.approvalStatus) = 'APPROVED' AND UPPER(s.storeStatus) = 'APPROVED'")
    List<Object[]> findPublicCatalogStores();

    @Query("SELECT MIN(p.proPriceOfSell), MAX(p.proPriceOfSell) FROM Product p LEFT JOIN p.store s WHERE p.isActive = true AND (p.listingSource IS NULL OR UPPER(p.listingSource) <> 'MARKETPLACE' OR (UPPER(p.approvalStatus) = 'APPROVED' AND UPPER(s.storeStatus) = 'APPROVED'))")
    Object[] findPublicCatalogPriceRange();
}
