package org.example.service;

import org.example.dto.PromotionRequest;
import org.example.model.Promotion;
import org.example.repository.PromotionRepository;
import org.example.repository.PromotionRedemptionRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromotionServiceTest {
    @Mock
    private PromotionRepository promotionRepository;

    @Mock
    private PromotionRedemptionRepository promotionRedemptionRepository;

    private PromotionService promotionService;

    @BeforeEach
    void setUp() {
        promotionService = new PromotionService(promotionRepository, promotionRedemptionRepository);
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
                "PERCENTAGE", 10d, 100d, 100d, 25, 1, startsAt, endsAt, true));

        assertEquals("SAVE10", result.code());
        assertEquals("PERCENTAGE", result.discountType());
        assertEquals(100d, result.maximumDiscountAmount());
        assertEquals(1, result.perUserLimit());
        assertEquals(0, result.usageCount());
    }

    @Test
    void checkoutRejectsAnExpiredPromotionWithoutConsumingIt() {
        Promotion promotion = promotion("SAVE10", "PERCENTAGE", 10d);
        promotion.setEndsAt(now().minusMinutes(1));
        when(promotionRepository.findByCodeForUpdate("SAVE10")).thenReturn(Optional.of(promotion));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> promotionService.applyForCheckout("save10", 7, 500d));

        assertEquals(400, exception.getStatusCode().value());
        assertEquals(0, promotion.getUsageCount());
    }

    @Test
    void checkoutCapsFixedDiscountAtTheCurrentServerSubtotalAndConsumesOnce() {
        Promotion promotion = promotion("SAVE500", "FIXED_AMOUNT", 500d);
        when(promotionRepository.findByCodeForUpdate("SAVE500")).thenReturn(Optional.of(promotion));

        var result = promotionService.applyForCheckout("save500", 7, 125d);

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
                () -> promotionService.preview(7, "SAVE10", 499d));

        assertEquals(400, exception.getStatusCode().value());
    }

    @Test
    void previewIncludesThePromotionRulesForTheBuyer() {
        Promotion promotion = promotion("HALF50", "PERCENTAGE", 50d);
        promotion.setPromotionId(8);
        promotion.setMaximumDiscountAmount(100d);
        promotion.setMinimumOrderAmount(500d);
        promotion.setPerUserLimit(1);
        when(promotionRepository.findByCodeIgnoreCase("HALF50")).thenReturn(Optional.of(promotion));
        when(promotionRedemptionRepository.countByPromotionIdAndUserId(8, 7)).thenReturn(0L);

        var preview = promotionService.preview(7, "half50", 500d);

        assertEquals("PERCENTAGE", preview.discountType());
        assertEquals(50d, preview.discountValue());
        assertEquals(100d, preview.maximumDiscountAmount());
        assertEquals(500d, preview.minimumOrderAmount());
        assertEquals(1, preview.perUserLimit());
    }

    @Test
    void percentagePromotionCapsTheDiscountAndRecordsTheCustomerRedemption() {
        Promotion promotion = promotion("HALF", "PERCENTAGE", 50d);
        promotion.setPromotionId(8);
        promotion.setMaximumDiscountAmount(100d);
        promotion.setPerUserLimit(1);
        when(promotionRepository.findByCodeForUpdate("HALF")).thenReturn(Optional.of(promotion));
        when(promotionRedemptionRepository.countByPromotionIdAndUserId(8, 7)).thenReturn(0L);

        var result = promotionService.applyForCheckout("half", 7, 500d);

        assertEquals(100d, result.discountAmount());
        verify(promotionRedemptionRepository).save(any());
    }

    @Test
    void previewRejectsAPromotionAlreadyUsedByThisCustomer() {
        Promotion promotion = promotion("ONCE", "PERCENTAGE", 10d);
        promotion.setPromotionId(8);
        promotion.setPerUserLimit(1);
        when(promotionRepository.findByCodeIgnoreCase("ONCE")).thenReturn(Optional.of(promotion));
        when(promotionRedemptionRepository.countByPromotionIdAndUserId(8, 7)).thenReturn(1L);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> promotionService.preview(7, "ONCE", 500d));

        assertEquals(400, exception.getStatusCode().value());
        assertEquals("You have reached the per-customer limit for this promotion", exception.getReason());
    }

    @Test
    void previewRejectsInactiveAndNotYetStartedPromotionsWithoutChangingUsage() {
        Promotion inactive = promotion("PAUSED", "PERCENTAGE", 10d);
        inactive.setActive(false);
        when(promotionRepository.findByCodeIgnoreCase("PAUSED")).thenReturn(Optional.of(inactive));

        ResponseStatusException inactiveException = assertThrows(ResponseStatusException.class,
                () -> promotionService.preview(7, "paused", 500d));

        assertEquals("This promotion is not active", inactiveException.getReason());
        assertEquals(0, inactive.getUsageCount());

        Promotion scheduled = promotion("SOON", "PERCENTAGE", 10d);
        scheduled.setStartsAt(now().plusMinutes(1));
        when(promotionRepository.findByCodeIgnoreCase("SOON")).thenReturn(Optional.of(scheduled));

        ResponseStatusException scheduledException = assertThrows(ResponseStatusException.class,
                () -> promotionService.preview(7, "soon", 500d));

        assertEquals("This promotion has not started yet", scheduledException.getReason());
        assertEquals(0, scheduled.getUsageCount());
    }

    @Test
    void previewRejectsAReachedGlobalUsageLimitWithoutCreatingARedemption() {
        Promotion promotion = promotion("FULL", "PERCENTAGE", 10d);
        promotion.setUsageLimit(2);
        promotion.setUsageCount(2);
        when(promotionRepository.findByCodeIgnoreCase("FULL")).thenReturn(Optional.of(promotion));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> promotionService.preview(7, "full", 500d));

        assertEquals("This promotion has reached its usage limit", exception.getReason());
        verify(promotionRedemptionRepository, never()).save(any());
    }

    @Test
    void createRejectsInvalidDiscountAndScheduleRules() {
        when(promotionRepository.existsByCodeIgnoreCase("TOOMUCH")).thenReturn(false);

        ResponseStatusException percentageException = assertThrows(ResponseStatusException.class, () ->
                promotionService.createPromotion(new PromotionRequest("TOOMUCH", "Too much", "PERCENTAGE",
                        101d, null, 0d, null, null, now().minusDays(1), now().plusDays(1), true)));

        assertEquals("Percentage discount cannot exceed 100", percentageException.getReason());

        when(promotionRepository.existsByCodeIgnoreCase("BADDATE")).thenReturn(false);
        ResponseStatusException dateException = assertThrows(ResponseStatusException.class, () ->
                promotionService.createPromotion(new PromotionRequest("BADDATE", "Bad date", "FIXED_AMOUNT",
                        10d, null, 0d, null, null, now(), now(), true)));

        assertEquals("Promotion end date must be after its start date", dateException.getReason());
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
