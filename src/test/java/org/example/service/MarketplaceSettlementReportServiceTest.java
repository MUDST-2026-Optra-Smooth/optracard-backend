package org.example.service;

import org.example.model.MarketplaceSettlementLedger;
import org.example.model.MarketplaceStore;
import org.example.model.SettlementEntryType;
import org.example.model.User;
import org.example.repository.MarketplaceSettlementLedgerRepository;
import org.example.repository.MarketplaceStoreRepository;
import org.example.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketplaceSettlementReportServiceTest {
    @Mock private MarketplaceSettlementLedgerRepository ledgerRepository;
    @Mock private MarketplaceStoreRepository storeRepository;
    @Mock private UserRepository userRepository;

    @Test
    void sellerReportUsesOnlyTheAuthenticatedSellersStoreAndIncludesReversalInTotals() {
        User seller = new User();
        seller.setUaId(7);
        MarketplaceStore store = new MarketplaceStore();
        store.setStoreId(9);
        store.setStoreStatus("APPROVED");
        when(userRepository.findByEmail("seller@example.test")).thenReturn(Optional.of(seller));
        when(storeRepository.findFirstBySellerUserIdOrderByStoreIdDesc(7)).thenReturn(Optional.of(store));

        MarketplaceSettlementLedger charge = entry(1, SettlementEntryType.ORDER_CHARGE, new BigDecimal("900.00"),
                new BigDecimal("45.00"), new BigDecimal("855.00"), null);
        MarketplaceSettlementLedger reversal = entry(2, SettlementEntryType.REVERSAL, new BigDecimal("-900.00"),
                new BigDecimal("-45.00"), new BigDecimal("-855.00"), 1);
        when(ledgerRepository.findByStoreIdOrderByCreatedAtDesc(9)).thenReturn(List.of(reversal, charge));

        var result = new MarketplaceSettlementReportService(ledgerRepository, storeRepository, userRepository)
                .sellerReport("seller@example.test", null, null, "ALL");

        assertEquals(new BigDecimal("0.00"), result.summary().grossAmount());
        assertEquals(new BigDecimal("0.00"), result.summary().feeAmount());
        assertEquals(new BigDecimal("0.00"), result.summary().netAmount());
        assertEquals("REVERSED", result.entries().get(1).status());
    }

    private MarketplaceSettlementLedger entry(Integer id, SettlementEntryType type, BigDecimal gross, BigDecimal fee,
                                              BigDecimal net, Integer reversalOfId) {
        MarketplaceSettlementLedger entry = new MarketplaceSettlementLedger();
        entry.setId(id);
        entry.setEntryType(type);
        entry.setStatus("POSTED");
        entry.setStoreId(9);
        entry.setStoreName("Card Corner");
        entry.setSellerUserId(7);
        entry.setReferenceType("ORDER");
        entry.setReferenceId("41");
        entry.setReferenceNumber("ORD-20261110-0041");
        entry.setGrossAmount(gross);
        entry.setFeeRate(new BigDecimal("5.00"));
        entry.setFeeAmount(fee);
        entry.setNetAmount(net);
        entry.setReversalOfId(reversalOfId);
        entry.setCreatedAt(LocalDateTime.now());
        return entry;
    }
}
