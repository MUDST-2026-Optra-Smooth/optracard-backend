package org.example.repository;

import org.example.model.PromotionRedemption;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromotionRedemptionRepository extends JpaRepository<PromotionRedemption, Integer> {
    long countByPromotionIdAndUserId(Integer promotionId, Integer userId);
}
