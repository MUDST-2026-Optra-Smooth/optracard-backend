package org.example.service;

import org.example.dto.PromotionRequest;
import org.example.model.Promotion;
import org.example.repository.PromotionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromotionServiceTest {
    @Mock
    private PromotionRepository promotionRepository;

    private PromotionService promotionService;

    @BeforeEach
    void setUp() {
        promotionService = new PromotionService(promotionRepository);
    }

    @Test
    void createNormalizesTheCodeAndValidatesAValidDateRange() {
        when(promotionRepository.existsByCodeIgnoreCase("SAVE10")).thenReturn(false);
        when(promotionRepository.saveAndFlush(any(Promotion.class))).thenAnswer(invocation -> {
            Promotion promotion = invocation.getArgument(0);
            promotion.setPromotionId(4);
            return promotion;
        });
        LocalDateTime startsAt = now().minusHours(1);
        LocalDateTime endsAt = now().plusDays(1);

        var result = promotionService.createPromotion(new PromotionRequest("save10", "Welcome discount",
                "PERCENTAGE", 10d, 100d, 25, startsAt, endsAt, true));

        assertEquals("SAVE10", result.code());
        assertEquals("PERCENTAGE", result.discountType());
        assertEquals(0, result.usageCount());
    }

    @Test
    void checkoutRejectsAnExpiredPromotionWithoutConsumingIt() {
        Promotion promotion = promotion("SAVE10", "PERCENTAGE", 10d);
        promotion.setEndsAt(now().minusMinutes(1));
        when(promotionRepository.findByCodeForUpdate("SAVE10")).thenReturn(Optional.of(promotion));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> promotionService.applyForCheckout("save10", 500d));

        assertEquals(400, exception.getStatusCode().value());
        assertEquals(0, promotion.getUsageCount());
    }

    @Test
    void checkoutCapsFixedDiscountAtTheCurrentServerSubtotalAndConsumesOnce() {
        Promotion promotion = promotion("SAVE500", "FIXED_AMOUNT", 500d);
        when(promotionRepository.findByCodeForUpdate("SAVE500")).thenReturn(Optional.of(promotion));

        var result = promotionService.applyForCheckout("save500", 125d);

        assertEquals(125d, result.discountAmount());
        assertEquals(1, promotion.getUsageCount());
        verify(promotionRepository).findByCodeForUpdate("SAVE500");
    }

    @Test
    void previewRejectsAPromotionBelowItsMinimumOrder() {
        Promotion promotion = promotion("SAVE10", "PERCENTAGE", 10d);
        promotion.setMinimumOrderAmount(500d);
        when(promotionRepository.findByCodeIgnoreCase("SAVE10")).thenReturn(Optional.of(promotion));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> promotionService.preview("SAVE10", 499d));

        assertEquals(400, exception.getStatusCode().value());
    }

    private Promotion promotion(String code, String type, double value) {
        Promotion promotion = new Promotion();
        promotion.setCode(code);
        promotion.setName("Promotion");
        promotion.setDiscountType(type);
        promotion.setDiscountValue(value);
        promotion.setMinimumOrderAmount(0d);
        promotion.setUsageCount(0);
        promotion.setActive(true);
        promotion.setStartsAt(now().minusDays(1));
        promotion.setEndsAt(now().plusDays(1));
        return promotion;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(ZoneId.of("Asia/Bangkok"));
    }
}
