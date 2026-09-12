package org.example.repository;

import org.example.model.Order;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Integer> {
    @EntityGraph(attributePaths = {"items"})
    List<Order> findByUserIdOrderByCreatedAtDesc(Integer userId);
}
