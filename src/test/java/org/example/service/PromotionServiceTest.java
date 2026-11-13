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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
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
        LocalDateTime sameMoment = now();
        ResponseStatusException dateException = assertThrows(ResponseStatusException.class, () ->
                promotionService.createPromotion(new PromotionRequest("BADDATE", "Bad date", "FIXED_AMOUNT",
                        10d, null, 0d, null, null, sameMoment, sameMoment, true)));

        assertEquals("Promotion end date must be after its start date", dateException.getReason());
    }

    @Test
    void listsGetsUpdatesAndDeletesPromotions() {
        Promotion existing = promotion("SAVE10", "PERCENTAGE", 10d);
        existing.setPromotionId(1);
        when(promotionRepository.findAllByOrderByEndsAtDesc()).thenReturn(List.of(existing));
        assertEquals(1, promotionService.getPromotions().size());

        when(promotionRepository.findById(1)).thenReturn(Optional.of(existing));
        assertEquals("SAVE10", promotionService.getPromotion(1).code());

        PromotionRequest request = validRequest("NEWCODE", "NEW NAME", "FIXED_AMOUNT", 25d);
        when(promotionRepository.findByCodeIgnoreCase("NEWCODE")).thenReturn(Optional.empty());
        when(promotionRepository.saveAndFlush(existing)).thenReturn(existing);
        assertEquals("NEWCODE", promotionService.updatePromotion(1, request).code());
        verify(promotionRepository).saveAndFlush(existing);

        promotionService.deletePromotion(1);
        verify(promotionRepository).delete(existing);

        when(promotionRepository.findById(404)).thenReturn(Optional.empty());
        ResponseStatusException missing = assertThrows(ResponseStatusException.class,
                () -> promotionService.getPromotion(404));
        assertEquals(404, missing.getStatusCode().value());
    }

    @Test
    void rejectsDuplicateCodesOnCreateAndUpdate() {
        when(promotionRepository.existsByCodeIgnoreCase("SAVE10")).thenReturn(true);
        ResponseStatusException createException = assertThrows(ResponseStatusException.class,
                () -> promotionService.createPromotion(validRequest("save10", "Duplicate", "PERCENTAGE", 10d)));
        assertEquals(409, createException.getStatusCode().value());

        Promotion existing = promotion("SAVE10", "PERCENTAGE", 10d);
        existing.setPromotionId(1);
        Promotion other = promotion("DUPLICATE", "PERCENTAGE", 10d);
        other.setPromotionId(2);
        when(promotionRepository.findById(1)).thenReturn(Optional.of(existing));
        when(promotionRepository.findByCodeIgnoreCase("DUPLICATE")).thenReturn(Optional.of(other));
        ResponseStatusException updateException = assertThrows(ResponseStatusException.class,
                () -> promotionService.updatePromotion(1, validRequest("duplicate", "Duplicate", "PERCENTAGE", 10d)));
        assertEquals(409, updateException.getStatusCode().value());
    }

    @Test
    void supportsBlankCheckoutAndRejectsUnknownCodes() {
        var none = promotionService.applyForCheckout("  ", 7, 100d);
        assertFalse(none.isApplied());
        verify(promotionRepository, never()).findByCodeForUpdate(anyString());

        when(promotionRepository.findByCodeForUpdate("UNKNOWN")).thenReturn(Optional.empty());
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> promotionService.applyForCheckout("unknown", 7, 100d));
        assertEquals("Promotion code is invalid", exception.getReason());
    }

    @Test
    void coversValidationRulesForPromotionRequests() {
        lenient().when(promotionRepository.existsByCodeIgnoreCase(anyString())).thenReturn(false);
        LocalDateTime start = now().minusHours(1);
        LocalDateTime end = now().plusDays(1);

        assertEquals("Promotion code is required", assertThrows(ResponseStatusException.class,
                () -> promotionService.createPromotion(null)).getReason());
        assertEquals("Promotion code is required", assertThrows(ResponseStatusException.class,
                () -> promotionService.createPromotion(new PromotionRequest(" ", "Name", "PERCENTAGE", 10d, null, null, null, null, start, end, true))).getReason());
        assertEquals("Promotion code must use 3-50 uppercase letters, numbers, hyphens, or underscores", assertThrows(ResponseStatusException.class,
                () -> promotionService.createPromotion(new PromotionRequest("NO!", "Name", "PERCENTAGE", 10d, null, null, null, null, start, end, true))).getReason());
        assertEquals("Promotion name is required", assertThrows(ResponseStatusException.class,
                () -> promotionService.createPromotion(new PromotionRequest("VALID", " ", "PERCENTAGE", 10d, null, null, null, null, start, end, true))).getReason());
        assertEquals("Discount type is required", assertThrows(ResponseStatusException.class,
                () -> promotionService.createPromotion(new PromotionRequest("VALID", "Name", " ", 10d, null, null, null, null, start, end, true))).getReason());
        assertEquals("Discount type must be PERCENTAGE or FIXED_AMOUNT", assertThrows(ResponseStatusException.class,
                () -> promotionService.createPromotion(new PromotionRequest("VALID", "Name", "OTHER", 10d, null, null, null, null, start, end, true))).getReason());
        assertEquals("Discount value is required", assertThrows(ResponseStatusException.class,
                () -> promotionService.createPromotion(new PromotionRequest("VALID", "Name", "PERCENTAGE", null, null, null, null, null, start, end, true))).getReason());
        assertEquals("Discount value is required", assertThrows(ResponseStatusException.class,
                () -> promotionService.createPromotion(new PromotionRequest("VALID", "Name", "PERCENTAGE", Double.NaN, null, null, null, null, start, end, true))).getReason());
        assertEquals("Discount value must be greater than zero", assertThrows(ResponseStatusException.class,
                () -> promotionService.createPromotion(new PromotionRequest("VALID", "Name", "PERCENTAGE", 0d, null, null, null, null, start, end, true))).getReason());
        assertEquals("Discount value is required", assertThrows(ResponseStatusException.class,
                () -> promotionService.createPromotion(new PromotionRequest("VALID", "Name", "PERCENTAGE", -1d, null, null, null, null, start, end, true))).getReason());
        assertEquals("Maximum discount must be zero or more", assertThrows(ResponseStatusException.class,
                () -> promotionService.createPromotion(new PromotionRequest("VALID", "Name", "PERCENTAGE", 10d, -1d, null, null, null, start, end, true))).getReason());
        assertEquals("Minimum order amount must be zero or more", assertThrows(ResponseStatusException.class,
                () -> promotionService.createPromotion(new PromotionRequest("VALID", "Name", "PERCENTAGE", 10d, null, -1d, null, null, start, end, true))).getReason());
        assertEquals("Usage limit must be at least 1", assertThrows(ResponseStatusException.class,
                () -> promotionService.createPromotion(new PromotionRequest("VALID", "Name", "PERCENTAGE", 10d, null, null, 0, null, start, end, true))).getReason());
        assertEquals("Per-customer limit must be at least 1", assertThrows(ResponseStatusException.class,
                () -> promotionService.createPromotion(new PromotionRequest("VALID", "Name", "PERCENTAGE", 10d, null, null, null, 0, start, end, true))).getReason());
        assertEquals("Promotion start and end dates are required", assertThrows(ResponseStatusException.class,
                () -> promotionService.createPromotion(new PromotionRequest("VALID", "Name", "PERCENTAGE", 10d, null, null, null, null, null, end, true))).getReason());
        assertEquals("Promotion start and end dates are required", assertThrows(ResponseStatusException.class,
                () -> promotionService.createPromotion(new PromotionRequest("VALID", "Name", "PERCENTAGE", 10d, null, null, null, null, start, null, true))).getReason());
    }

    @Test
    void coversDiscountCapsNullCountsAndResponseRounding() {
        Promotion percentage = promotion("PCT", "PERCENTAGE", 12.345d);
        percentage.setUsageCount(null);
        percentage.setUsageLimit(5);
        percentage.setMaximumDiscountAmount(null);
        percentage.setPromotionId(5);
        when(promotionRepository.findByCodeForUpdate("PCT")).thenReturn(Optional.of(percentage));

        var result = promotionService.applyForCheckout("pct", 7, 100d);
        assertTrue(result.isApplied());
        assertEquals(12.35d, result.discountAmount());
        assertEquals(1, percentage.getUsageCount());

        Promotion fixed = promotion("FIXED", "FIXED_AMOUNT", 200d);
        fixed.setPromotionId(6);
        fixed.setUsageCount(0);
        when(promotionRepository.findByCodeIgnoreCase("FIXED")).thenReturn(Optional.of(fixed));
        var preview = promotionService.preview(7, "fixed", 500d);
        assertEquals(200d, preview.discountAmount());
        assertEquals(300d, preview.discountedSubtotal());
    }

    private PromotionRequest validRequest(String code, String name, String type, double value) {
        return new PromotionRequest(code, name, type, value, null, 0d, null, null,
                now().minusHours(1), now().plusDays(1), true);
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
