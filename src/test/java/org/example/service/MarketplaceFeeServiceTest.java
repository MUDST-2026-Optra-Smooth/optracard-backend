package org.example.service;

import org.example.dto.UpdateMarketplaceFeeRequest;
import org.example.model.MarketplaceFeeConfig;
import org.example.repository.MarketplaceFeeConfigRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketplaceFeeServiceTest {
    @Mock private MarketplaceFeeConfigRepository configRepository;

    @Test
    void updateCreatesANewFeeRateVersionInsteadOfMutatingHistory() {
        MarketplaceFeeService service = new MarketplaceFeeService(configRepository);
        when(configRepository.saveAndFlush(any(MarketplaceFeeConfig.class))).thenAnswer(invocation -> {
            MarketplaceFeeConfig config = invocation.getArgument(0);
            config.setId(2);
            return config;
        });

        var response = service.update(new UpdateMarketplaceFeeRequest(new BigDecimal("7.25")), "admin@example.test");

        ArgumentCaptor<MarketplaceFeeConfig> captor = ArgumentCaptor.forClass(MarketplaceFeeConfig.class);
        verify(configRepository).saveAndFlush(captor.capture());
        assertEquals(new BigDecimal("7.25"), captor.getValue().getFeeRate());
        assertEquals("admin@example.test", captor.getValue().getChangedBy());
        assertEquals(new BigDecimal("7.25"), response.feeRate());
    }

    @Test
    void updateRejectsRateOutsideAllowedRange() {
        MarketplaceFeeService service = new MarketplaceFeeService(configRepository);

        assertThrows(ResponseStatusException.class,
                () -> service.update(new UpdateMarketplaceFeeRequest(new BigDecimal("100.01")), "admin@example.test"));
    }

    @Test
    void currentConfigurationCreatesTheAuditableDefaultWhenNoHistoryExists() {
        MarketplaceFeeService service = new MarketplaceFeeService(configRepository);
        when(configRepository.findFirstByEffectiveFromLessThanEqualOrderByEffectiveFromDesc(any())).thenReturn(Optional.empty());
        when(configRepository.saveAndFlush(any(MarketplaceFeeConfig.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MarketplaceFeeConfig result = service.currentConfiguration();

        assertEquals(new BigDecimal("5.00"), result.getFeeRate());
        assertEquals("system", result.getChangedBy());
        verify(configRepository).saveAndFlush(any(MarketplaceFeeConfig.class));
    }

    @Test
    void updateRoundsToMoneyPrecisionAndNormalizesBlankActor() {
        MarketplaceFeeService service = new MarketplaceFeeService(configRepository);
        when(configRepository.saveAndFlush(any(MarketplaceFeeConfig.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.update(new UpdateMarketplaceFeeRequest(new BigDecimal("6.666")), "  ");

        assertEquals(new BigDecimal("6.67"), response.feeRate());
        assertEquals("system", response.changedBy());
    }
}
