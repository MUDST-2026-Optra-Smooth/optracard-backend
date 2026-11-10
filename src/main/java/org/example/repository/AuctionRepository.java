package org.example.repository;

import jakarta.persistence.LockModeType;
import org.example.model.Auction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AuctionRepository extends JpaRepository<Auction, Integer> {
    boolean existsByTitle(String title);
    List<Auction> findAllByOrderByCreatedAtDesc();
    List<Auction> findBySellerUserIdOrderByCreatedAtDesc(Integer sellerUserId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Auction a where a.auctionId = :id")
    Optional<Auction> findByIdForUpdate(Integer id);
}
