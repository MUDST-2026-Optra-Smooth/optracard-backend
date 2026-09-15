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

    Optional<Product> findByProSku(String proSku);

    @Query("SELECT p FROM Product p LEFT JOIN p.cardGame g WHERE " +
            "LOWER(p.proName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(g.gameName) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Product> searchByKeyword(@Param("keyword") String keyword);
}
