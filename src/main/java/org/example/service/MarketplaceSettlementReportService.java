package org.example.service;

import org.example.dto.SettlementLedgerEntryResponse;
import org.example.dto.SettlementReportResponse;
import org.example.dto.SettlementSummaryResponse;
import org.example.model.MarketplaceSettlementLedger;
import org.example.model.MarketplaceStore;
import org.example.model.SettlementEntryType;
import org.example.model.User;
import org.example.repository.MarketplaceSettlementLedgerRepository;
import org.example.repository.MarketplaceStoreRepository;
import org.example.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class MarketplaceSettlementReportService {
    private final MarketplaceSettlementLedgerRepository ledgerRepository;
    private final MarketplaceStoreRepository storeRepository;
    private final UserRepository userRepository;

    public MarketplaceSettlementReportService(MarketplaceSettlementLedgerRepository ledgerRepository,
                                              MarketplaceStoreRepository storeRepository,
                                              UserRepository userRepository) {
        this.ledgerRepository = ledgerRepository;
        this.storeRepository = storeRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public SettlementReportResponse sellerReport(String email, LocalDate from, LocalDate to, String type) {
        MarketplaceStore store = requireApprovedStore(email);
        return build(ledgerRepository.findByStoreIdOrderByCreatedAtDesc(store.getStoreId()), from, to, type, null, null);
    }

    @Transactional(readOnly = true)
    public SettlementReportResponse superAdminReport(LocalDate from, LocalDate to, String type,
                                                      Integer sellerUserId, Integer storeId) {
        return build(ledgerRepository.findAllByOrderByCreatedAtDesc(), from, to, type, sellerUserId, storeId);
    }

    private SettlementReportResponse build(List<MarketplaceSettlementLedger> source, LocalDate from, LocalDate to,
                                           String rawType, Integer sellerUserId, Integer storeId) {
        validateDates(from, to);
        SettlementEntryType type = parseType(rawType);
        LocalDateTime start = from == null ? null : from.atStartOfDay();
        LocalDateTime endExclusive = to == null ? null : to.plusDays(1).atStartOfDay();
        List<MarketplaceSettlementLedger> entries = source.stream()
                .filter(entry -> type == null || entry.getEntryType() == type)
                .filter(entry -> sellerUserId == null || sellerUserId.equals(entry.getSellerUserId()))
                .filter(entry -> storeId == null || storeId.equals(entry.getStoreId()))
                .filter(entry -> start == null || !entry.getCreatedAt().isBefore(start))
                .filter(entry -> endExclusive == null || entry.getCreatedAt().isBefore(endExclusive))
                .toList();

        Set<Integer> reversedChargeIds = new HashSet<>();
        entries.stream().map(MarketplaceSettlementLedger::getReversalOfId)
                .filter(id -> id != null).forEach(reversedChargeIds::add);
        List<SettlementLedgerEntryResponse> responseEntries = entries.stream()
                .map(entry -> toResponse(entry, reversedChargeIds.contains(entry.getId())))
                .toList();

        BigDecimal gross = total(entries, MarketplaceSettlementLedger::getGrossAmount);
        BigDecimal fee = total(entries, MarketplaceSettlementLedger::getFeeAmount);
        BigDecimal net = total(entries, MarketplaceSettlementLedger::getNetAmount);
        return new SettlementReportResponse(new SettlementSummaryResponse(gross, fee, net), responseEntries);
    }

    private SettlementLedgerEntryResponse toResponse(MarketplaceSettlementLedger entry, boolean reversed) {
        String status = reversed && entry.getEntryType() != SettlementEntryType.REVERSAL ? "REVERSED" : entry.getStatus();
        return new SettlementLedgerEntryResponse(entry.getId(), entry.getEntryType().name(), status,
                entry.getSellerUserId(), entry.getStoreId(), entry.getStoreName(), entry.getReferenceType(),
                entry.getReferenceId(), entry.getReferenceNumber(), entry.getGrossAmount(), entry.getFeeRate(),
                entry.getFeeAmount(), entry.getNetAmount(), entry.getReversalOfId(), entry.getCreatedAt());
    }

    private BigDecimal total(List<MarketplaceSettlementLedger> entries,
                             java.util.function.Function<MarketplaceSettlementLedger, BigDecimal> amount) {
        return entries.stream().map(amount).filter(value -> value != null).reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private MarketplaceStore requireApprovedStore(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        MarketplaceStore store = storeRepository.findFirstBySellerUserIdOrderByStoreIdDesc(user.getUaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "An approved seller store is required"));
        if (!"APPROVED".equalsIgnoreCase(store.getStoreStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Your seller store is not approved yet");
        }
        return store;
    }

    private SettlementEntryType parseType(String rawType) {
        if (rawType == null || rawType.isBlank() || "ALL".equalsIgnoreCase(rawType)) return null;
        try {
            return SettlementEntryType.valueOf(rawType.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid settlement type");
        }
    }

    private void validateDates(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "From date cannot be after to date");
        }
    }
}
