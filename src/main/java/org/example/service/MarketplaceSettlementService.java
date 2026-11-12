package org.example.service;

import org.example.model.MarketplaceFeeConfig;
import org.example.model.MarketplaceSettlementLedger;
import org.example.model.MarketplaceStore;
import org.example.model.Order;
import org.example.model.SettlementEntryType;
import org.example.repository.MarketplaceSettlementLedgerRepository;
import org.example.repository.MarketplaceStoreRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/** Records immutable Marketplace charges and their compensating reversals. */
@Service
public class MarketplaceSettlementService {
    private static final String ORDER_REFERENCE = "ORDER";
    private static final String AUCTION_REFERENCE = "AUCTION";

    private final MarketplaceSettlementLedgerRepository ledgerRepository;
    private final MarketplaceStoreRepository storeRepository;
    private final MarketplaceFeeService feeService;

    public MarketplaceSettlementService(MarketplaceSettlementLedgerRepository ledgerRepository,
                                        MarketplaceStoreRepository storeRepository,
                                        MarketplaceFeeService feeService) {
        this.ledgerRepository = ledgerRepository;
        this.storeRepository = storeRepository;
        this.feeService = feeService;
    }

    /** Creates at most one charge for a paid Marketplace order. */
    @Transactional
    public Optional<MarketplaceSettlementLedger> recordOrderCharge(Order order) {
        if (!isPaidMarketplaceOrder(order)) return Optional.empty();
        String referenceId = String.valueOf(order.getOrderId());
        Optional<MarketplaceSettlementLedger> existing = ledgerRepository
                .findByEntryTypeAndReferenceTypeAndReferenceId(SettlementEntryType.ORDER_CHARGE, ORDER_REFERENCE, referenceId);
        if (existing.isPresent()) return existing;

        MarketplaceStore store = requireStore(order.getStoreId());
        MarketplaceFeeConfig configuration = feeService.currentConfiguration();
        BigDecimal gross = orderGrossAfterDiscount(order);
        return Optional.of(ledgerRepository.saveAndFlush(createCharge(
                SettlementEntryType.ORDER_CHARGE, ORDER_REFERENCE, referenceId, orderNumber(order),
                store.getSellerUserId(), store.getStoreId(), store.getStoreName(), gross, configuration.getFeeRate())));
    }

    /** Adds exactly one negative entry that compensates the original order charge. */
    @Transactional
    public Optional<MarketplaceSettlementLedger> recordOrderReversal(Order order) {
        if (!isMarketplaceOrder(order) || order.getOrderId() == null) return Optional.empty();
        String referenceId = String.valueOf(order.getOrderId());
        Optional<MarketplaceSettlementLedger> original = ledgerRepository
                .findByEntryTypeAndReferenceTypeAndReferenceId(SettlementEntryType.ORDER_CHARGE, ORDER_REFERENCE, referenceId);
        if (original.isEmpty()) return Optional.empty();
        Optional<MarketplaceSettlementLedger> existingReversal = ledgerRepository.findByReversalOfId(original.get().getId());
        if (existingReversal.isPresent()) return existingReversal;

        MarketplaceSettlementLedger charge = original.get();
        MarketplaceSettlementLedger reversal = new MarketplaceSettlementLedger();
        reversal.setEntryType(SettlementEntryType.REVERSAL);
        reversal.setStatus("POSTED");
        reversal.setSellerUserId(charge.getSellerUserId());
        reversal.setStoreId(charge.getStoreId());
        reversal.setStoreName(charge.getStoreName());
        reversal.setReferenceType(ORDER_REFERENCE);
        reversal.setReferenceId(referenceId);
        reversal.setReferenceNumber(charge.getReferenceNumber());
        reversal.setGrossAmount(charge.getGrossAmount().negate());
        reversal.setFeeRate(charge.getFeeRate());
        reversal.setFeeAmount(charge.getFeeAmount().negate());
        reversal.setNetAmount(charge.getNetAmount().negate());
        reversal.setReversalOfId(charge.getId());
        return Optional.of(ledgerRepository.saveAndFlush(reversal));
    }

    /** Future Auction checkout can call this same rule without any promotion input. */
    @Transactional
    public MarketplaceSettlementLedger recordAuctionCharge(AuctionSettlementCharge auction) {
        if (auction == null || auction.storeId() == null || auction.winningPrice() == null
                || auction.winningPrice().compareTo(BigDecimal.ZERO) < 0 || blank(auction.auctionReference())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valid auction settlement data is required");
        }
        Optional<MarketplaceSettlementLedger> existing = ledgerRepository.findByEntryTypeAndReferenceTypeAndReferenceId(
                SettlementEntryType.AUCTION_CHARGE, AUCTION_REFERENCE, auction.auctionReference().trim());
        if (existing.isPresent()) return existing.get();

        MarketplaceStore store = requireStore(auction.storeId());
        MarketplaceFeeConfig configuration = feeService.currentConfiguration();
        BigDecimal gross = money(auction.winningPrice());
        return ledgerRepository.saveAndFlush(createCharge(SettlementEntryType.AUCTION_CHARGE, AUCTION_REFERENCE,
                auction.auctionReference().trim(), auction.auctionReference().trim(), store.getSellerUserId(),
                store.getStoreId(), store.getStoreName(), gross, configuration.getFeeRate()));
    }

    private MarketplaceSettlementLedger createCharge(SettlementEntryType type, String referenceType, String referenceId,
                                                      String referenceNumber, Integer sellerUserId, Integer storeId,
                                                      String storeName, BigDecimal gross, BigDecimal feeRate) {
        BigDecimal normalizedRate = feeRate.setScale(2, RoundingMode.HALF_UP);
        BigDecimal fee = money(gross.multiply(normalizedRate).divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP));
        MarketplaceSettlementLedger entry = new MarketplaceSettlementLedger();
        entry.setEntryType(type);
        entry.setStatus("POSTED");
        entry.setSellerUserId(sellerUserId);
        entry.setStoreId(storeId);
        entry.setStoreName(storeName);
        entry.setReferenceType(referenceType);
        entry.setReferenceId(referenceId);
        entry.setReferenceNumber(referenceNumber);
        entry.setGrossAmount(money(gross));
        entry.setFeeRate(normalizedRate);
        entry.setFeeAmount(fee);
        entry.setNetAmount(money(gross.subtract(fee)));
        return entry;
    }

    private BigDecimal orderGrossAfterDiscount(Order order) {
        if (order.getSubtotal() != null) {
            return money(decimal(order.getSubtotal()).subtract(decimal(order.getDiscountAmount())).max(BigDecimal.ZERO));
        }
        // Legacy orders do not have subtotal snapshots. Their total included shipping,
        // so remove that amount and never charge a fee on it.
        return money(decimal(order.getTotalPrice()).subtract(decimal(order.getShippingFee())).max(BigDecimal.ZERO));
    }

    private boolean isPaidMarketplaceOrder(Order order) {
        return isMarketplaceOrder(order) && "PAID".equalsIgnoreCase(order.getPaymentStatus());
    }

    private boolean isMarketplaceOrder(Order order) {
        return order != null && order.getOrderId() != null && order.getStoreId() != null
                && "MARKETPLACE".equalsIgnoreCase(order.getSource());
    }

    private MarketplaceStore requireStore(Integer storeId) {
        return storeRepository.findById(storeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "Marketplace store is unavailable for settlement"));
    }

    private String orderNumber(Order order) {
        String date = order.getCreatedAt() == null ? LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                : order.getCreatedAt().toLocalDate().format(DateTimeFormatter.BASIC_ISO_DATE);
        return "ORD-" + date + "-" + String.format("%04d", order.getOrderId());
    }

    private BigDecimal decimal(Double value) {
        return value == null ? BigDecimal.ZERO : BigDecimal.valueOf(value);
    }

    private BigDecimal money(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public record AuctionSettlementCharge(Integer storeId, String auctionReference, BigDecimal winningPrice) {}
}
