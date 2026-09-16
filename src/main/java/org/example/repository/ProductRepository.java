package org.example.repository;

import org.example.model.Product;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {

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
}
