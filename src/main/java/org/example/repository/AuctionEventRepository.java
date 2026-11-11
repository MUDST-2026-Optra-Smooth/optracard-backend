package org.example.repository;
import org.example.model.AuctionEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface AuctionEventRepository extends JpaRepository<AuctionEvent,Long> {
 @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
 @org.springframework.data.jpa.repository.Query("select e from AuctionEvent e where e.id = :id")
 java.util.Optional<AuctionEvent> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") Long id);
 List<AuctionEvent> findByRecipientIdOrderByCreatedAtDesc(Integer recipientId);
 List<AuctionEvent> findByAuctionIdOrderByCreatedAtDesc(Integer auctionId);
 List<AuctionEvent> findByKindOrderByCreatedAtDesc(String kind);
 boolean existsByDedupeKey(String dedupeKey);
}
