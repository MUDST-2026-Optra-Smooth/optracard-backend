package org.example.repository;

import org.example.model.MarketplaceFeeConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MarketplaceFeeConfigRepository extends JpaRepository<MarketplaceFeeConfig, Integer> {
    Optional<MarketplaceFeeConfig> findFirstByEffectiveFromLessThanEqualOrderByEffectiveFromDesc(LocalDateTime now);
    List<MarketplaceFeeConfig> findAllByOrderByEffectiveFromDesc();
}
