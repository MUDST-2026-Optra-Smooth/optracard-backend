package org.example.repository;

import org.example.model.Product;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {

    @EntityGraph(attributePaths = {"cardGame", "store"})
    List<Product> findByIsActiveTrueOrderByProTypeAscProIdAsc();

    Optional<Product> findByProSku(String proSku);
}
