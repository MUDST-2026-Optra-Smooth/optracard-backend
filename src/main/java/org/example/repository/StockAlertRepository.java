package org.example.repository;

import org.example.model.StockAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface StockAlertRepository extends JpaRepository<StockAlert, Integer> {
    List<StockAlert> findByProductIdIn(Collection<Integer> productIds);
    List<StockAlert> findByResolvedAtIsNullAndProductIdIn(Collection<Integer> productIds);
}
