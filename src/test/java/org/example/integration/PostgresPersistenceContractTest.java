package org.example.integration;

import org.example.model.Promotion;
import org.example.model.Auction;
import org.example.model.AuctionStatus;
import org.example.model.MarketplaceSettlementLedger;
import org.example.model.SettlementEntryType;
import org.example.repository.AuctionRepository;
import org.example.repository.MarketplaceSettlementLedgerRepository;
import org.example.repository.PromotionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Guards the PostgreSQL mapping separately from the fast H2 test suite.
 * It is intentionally opt-in so pull requests do not require a Docker daemon.
 */
@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
        "spring.datasource.driver-class-name=org.postgresql.Driver",
        // This contract test starts the complete application context without the
        // test profile, so it must supply the required security property itself.
        "jwt.secret=test-secret-key-that-is-long-enough-for-hs256-signing-optracard-2026"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
@EnabledIfEnvironmentVariable(named = "RUN_POSTGRES_TESTS", matches = "true")
class PostgresPersistenceContractTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("optracard_contract")
            .withUsername("contract")
            .withPassword("contract");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private PromotionRepository promotionRepository;

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private MarketplaceSettlementLedgerRepository ledgerRepository;

    @Test
    void persistsPromotionRulesAndReadsThemBackWithPostgreSqlTypes() {
        Promotion promotion = new Promotion();
        promotion.setCode("E2E_PG_RULES");
        promotion.setName("PostgreSQL contract rule");
        promotion.setDiscountType("PERCENTAGE");
        promotion.setDiscountValue(50d);
        promotion.setMaximumDiscountAmount(100d);
        promotion.setMinimumOrderAmount(500d);
        promotion.setPerUserLimit(1);
        promotion.setUsageCount(0);
        promotion.setActive(true);
        promotion.setStartsAt(LocalDateTime.now().minusHours(1));
        promotion.setEndsAt(LocalDateTime.now().plusHours(1));

        Promotion saved = promotionRepository.saveAndFlush(promotion);
        Promotion restored = promotionRepository.findById(saved.getPromotionId()).orElseThrow();

        assertNotNull(restored.getPromotionId());
        assertEquals("E2E_PG_RULES", restored.getCode());
        assertEquals(100d, restored.getMaximumDiscountAmount());
        assertEquals(500d, restored.getMinimumOrderAmount());
        assertEquals(1, restored.getPerUserLimit());
    }

    @Test
    void persistsAuctionLifecycleAndSettlementDecimalSnapshotWithPostgreSqlConstraints() {
        Auction auction = new Auction();
        auction.setSellerUserId(7);
        auction.setSource("MARKETPLACE");
        auction.setStatus(AuctionStatus.LIVE.name());
        auction.setTitle("E2E-PG-Auction-" + System.nanoTime());
        auction.setStartingPrice(999.99);
        auction.setCurrentPrice(1050.49);
        auction.setBidCount(2);
        auction.setStartsAt(LocalDateTime.now().minusHours(1));
        auction.setEndsAt(LocalDateTime.now().plusHours(2));
        Auction savedAuction = auctionRepository.saveAndFlush(auction);

        MarketplaceSettlementLedger charge = ledger("AUC-" + savedAuction.getAuctionId(), SettlementEntryType.AUCTION_CHARGE);
        MarketplaceSettlementLedger savedCharge = ledgerRepository.saveAndFlush(charge);
        MarketplaceSettlementLedger restored = ledgerRepository.findByEntryTypeAndReferenceTypeAndReferenceId(
                SettlementEntryType.AUCTION_CHARGE, "AUCTION", charge.getReferenceId()).orElseThrow();

        assertNotNull(savedAuction.getAuctionId());
        assertEquals(AuctionStatus.LIVE.name(), savedAuction.getStatus());
        assertEquals(new BigDecimal("72.50"), restored.getFeeAmount());
        assertEquals(new BigDecimal("927.49"), restored.getNetAmount());

        MarketplaceSettlementLedger duplicate = ledger(charge.getReferenceId(), SettlementEntryType.AUCTION_CHARGE);
        assertThrows(DataIntegrityViolationException.class, () -> ledgerRepository.saveAndFlush(duplicate));
        assertNotNull(savedCharge.getId());
    }

    private MarketplaceSettlementLedger ledger(String referenceId, SettlementEntryType type) {
        MarketplaceSettlementLedger entry = new MarketplaceSettlementLedger();
        entry.setEntryType(type);
        entry.setSellerUserId(7);
        entry.setStoreId(9);
        entry.setStoreName("PostgreSQL Contract Store");
        entry.setReferenceType("AUCTION");
        entry.setReferenceId(referenceId);
        entry.setReferenceNumber(referenceId);
        entry.setGrossAmount(new BigDecimal("999.99"));
        entry.setFeeRate(new BigDecimal("7.25"));
        entry.setFeeAmount(new BigDecimal("72.50"));
        entry.setNetAmount(new BigDecimal("927.49"));
        return entry;
    }
}
