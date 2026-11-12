package org.example.service;

import org.example.dto.MarketplaceFeeConfigResponse;
import org.example.dto.UpdateMarketplaceFeeRequest;
import org.example.model.MarketplaceFeeConfig;
import org.example.repository.MarketplaceFeeConfigRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class MarketplaceFeeService {
    public static final BigDecimal DEFAULT_FEE_RATE = new BigDecimal("5.00");

    private final MarketplaceFeeConfigRepository configRepository;

    public MarketplaceFeeService(MarketplaceFeeConfigRepository configRepository) {
        this.configRepository = configRepository;
    }

    @Transactional
    public MarketplaceFeeConfig currentConfiguration() {
        return configRepository.findFirstByEffectiveFromLessThanEqualOrderByEffectiveFromDesc(LocalDateTime.now())
                .orElseGet(this::createDefaultConfiguration);
    }

    @Transactional
    public MarketplaceFeeConfigResponse currentResponse() {
        return toResponse(currentConfiguration());
    }

    @Transactional
    public List<MarketplaceFeeConfigResponse> history() {
        // Make a fresh database expose the default 5% row consistently even
        // when the current-rate and history requests arrive concurrently.
        currentConfiguration();
        return configRepository.findAllByOrderByEffectiveFromDesc().stream().map(this::toResponse).toList();
    }

    /** Adds a version instead of mutating an old rate so existing charges stay auditable. */
    @Transactional
    public MarketplaceFeeConfigResponse update(UpdateMarketplaceFeeRequest request, String changedBy) {
        if (request == null || request.feeRate() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fee rate is required");
        }
        BigDecimal rate = request.feeRate().setScale(2, RoundingMode.HALF_UP);
        if (rate.compareTo(BigDecimal.ZERO) < 0 || rate.compareTo(new BigDecimal("100.00")) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fee rate must be between 0 and 100");
        }
        MarketplaceFeeConfig configuration = new MarketplaceFeeConfig();
        configuration.setFeeRate(rate);
        configuration.setEffectiveFrom(LocalDateTime.now());
        configuration.setChangedBy(normalizeActor(changedBy));
        return toResponse(configRepository.saveAndFlush(configuration));
    }

    private MarketplaceFeeConfig createDefaultConfiguration() {
        MarketplaceFeeConfig configuration = new MarketplaceFeeConfig();
        configuration.setFeeRate(DEFAULT_FEE_RATE);
        configuration.setEffectiveFrom(LocalDateTime.now());
        configuration.setChangedBy("system");
        return configRepository.saveAndFlush(configuration);
    }

    private MarketplaceFeeConfigResponse toResponse(MarketplaceFeeConfig configuration) {
        return new MarketplaceFeeConfigResponse(configuration.getId(), configuration.getFeeRate(),
                configuration.getEffectiveFrom(), configuration.getChangedBy(), configuration.getCreatedAt());
    }

    private String normalizeActor(String actor) {
        if (actor == null || actor.trim().isEmpty()) return "system";
        return actor.trim();
    }
}
