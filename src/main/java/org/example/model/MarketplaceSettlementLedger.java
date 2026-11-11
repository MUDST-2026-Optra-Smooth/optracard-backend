package org.example.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Append-only accounting ledger for Marketplace seller earnings. Reversals are
 * separate, negative entries linked to the original charge rather than deletes.
 */
@Entity
@Getter
@Setter
@Table(name = "marketplace_settlement_ledger", uniqueConstraints = {
        @UniqueConstraint(name = "uk_settlement_charge_reference",
                columnNames = {"settlement_entry_type", "reference_type", "reference_id"}),
        @UniqueConstraint(name = "uk_settlement_reversal_of", columnNames = "reversal_of_id")
})
public class MarketplaceSettlementLedger {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "settlement_id")
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(name = "settlement_entry_type", nullable = false, length = 30)
    private SettlementEntryType entryType;

    @Column(name = "settlement_status", nullable = false, length = 30)
    private String status = "POSTED";

    @Column(name = "seller_user_id", nullable = false)
    private Integer sellerUserId;

    @Column(name = "store_id", nullable = false)
    private Integer storeId;

    @Column(name = "store_name", nullable = false, length = 255)
    private String storeName;

    @Column(name = "reference_type", nullable = false, length = 30)
    private String referenceType;

    @Column(name = "reference_id", nullable = false, length = 100)
    private String referenceId;

    @Column(name = "reference_number", nullable = false, length = 100)
    private String referenceNumber;

    /** Gross marketplace product value after promotion and before fee. */
    @Column(name = "gross_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal grossAmount;

    /** Immutable percentage snapshot used to create this ledger entry. */
    @Column(name = "fee_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal feeRate;

    @Column(name = "fee_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal feeAmount;

    @Column(name = "net_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal netAmount;

    @Column(name = "reversal_of_id")
    private Integer reversalOfId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (status == null || status.isBlank()) status = "POSTED";
    }
}
