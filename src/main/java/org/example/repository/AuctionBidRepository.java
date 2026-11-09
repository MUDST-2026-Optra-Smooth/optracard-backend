package org.example.repository;

import org.example.model.AuctionBid;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AuctionBidRepository extends JpaRepository<AuctionBid, Integer> {
    List<AuctionBid> findByAuction_AuctionIdOrderByCreatedAtDesc(Integer auctionId);
    Optional<AuctionBid> findFirstByAuction_AuctionIdOrderByAmountDescCreatedAtAsc(Integer auctionId);
    Optional<AuctionBid> findFirstByAuction_AuctionIdAndBidderUserId(Integer auctionId, Integer bidderUserId);
}
