package org.example.repository;

import org.example.model.StockAlertRead;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StockAlertReadRepository extends JpaRepository<StockAlertRead, Integer> {
    List<StockAlertRead> findByUserIdAndAlertIdIn(Integer userId, Collection<Integer> alertIds);
    Optional<StockAlertRead> findByAlertIdAndUserId(Integer alertId, Integer userId);
    void deleteByAlertIdAndUserId(Integer alertId, Integer userId);
}
