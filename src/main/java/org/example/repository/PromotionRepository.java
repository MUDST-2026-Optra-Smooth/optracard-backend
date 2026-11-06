package org.example.repository;

import jakarta.persistence.LockModeType;
import org.example.model.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PromotionRepository extends JpaRepository<Promotion, Integer> {
    List<Promotion> findAllByOrderByEndsAtDesc();

    Optional<Promotion> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select promotion from Promotion promotion where upper(promotion.code) = upper(:code)")
    Optional<Promotion> findByCodeForUpdate(@Param("code") String code);
}
