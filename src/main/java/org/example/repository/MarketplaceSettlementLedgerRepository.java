package org.example.repository;

import org.example.model.MarketplaceSettlementLedger;
import org.example.model.SettlementEntryType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MarketplaceSettlementLedgerRepository extends JpaRepository<MarketplaceSettlementLedger, Integer> {
    Optional<MarketplaceSettlementLedger> findByEntryTypeAndReferenceTypeAndReferenceId(
            SettlementEntryType entryType, String referenceType, String referenceId);
    Optional<MarketplaceSettlementLedger> findByReversalOfId(Integer reversalOfId);
    List<MarketplaceSettlementLedger> findAllByOrderByCreatedAtDesc();
    List<MarketplaceSettlementLedger> findByStoreIdOrderByCreatedAtDesc(Integer storeId);
}
