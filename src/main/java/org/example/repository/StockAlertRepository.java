package org.example.repository;

import org.example.model.StockAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StockAlertRepository extends JpaRepository<StockAlert, Integer> {
    Optional<StockAlert> findByProductId(Integer productId);
    List<StockAlert> findByResolvedAtIsNullAndProductIdIn(Collection<Integer> productIds);
}
