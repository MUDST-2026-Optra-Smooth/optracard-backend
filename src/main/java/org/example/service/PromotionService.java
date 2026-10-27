package org.example.service;

import org.example.dto.PromotionPreviewResponse;
import org.example.dto.PromotionRequest;
import org.example.dto.PromotionResponse;
import org.example.model.Promotion;
import org.example.repository.PromotionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;

@Service
public class PromotionService {
    public static final String PERCENTAGE = "PERCENTAGE";
    public static final String FIXED_AMOUNT = "FIXED_AMOUNT";
    private static final ZoneId PROMOTION_TIME_ZONE = ZoneId.of("Asia/Bangkok");

    private final PromotionRepository promotionRepository;

    public PromotionService(PromotionRepository promotionRepository) {
        this.promotionRepository = promotionRepository;
    }

    @Transactional(readOnly = true)
    public List<PromotionResponse> getPromotions() {
        return promotionRepository.findAllByOrderByEndsAtDesc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PromotionResponse getPromotion(Integer promotionId) {
        return toResponse(findPromotion(promotionId));
    }

    @Transactional
    public PromotionResponse createPromotion(PromotionRequest request) {
        String code = normalizeCode(request == null ? null : request.code());
        if (promotionRepository.existsByCodeIgnoreCase(code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Promotion code already exists");
        }
        Promotion promotion = new Promotion();
        applyRequest(promotion, request, code);
        return toResponse(promotionRepository.saveAndFlush(promotion));
    }

    @Transactional
    public PromotionResponse updatePromotion(Integer promotionId, PromotionRequest request) {
        Promotion promotion = findPromotion(promotionId);
        String code = normalizeCode(request == null ? null : request.code());
        promotionRepository.findByCodeIgnoreCase(code)
                .filter(existing -> !existing.getPromotionId().equals(promotionId))
                .ifPresent(existing -> { throw new ResponseStatusException(HttpStatus.CONFLICT, "Promotion code already exists"); });
        applyRequest(promotion, request, code);
        return toResponse(promotionRepository.saveAndFlush(promotion));
    }

    @Transactional
    public void deletePromotion(Integer promotionId) {
        promotionRepository.delete(findPromotion(promotionId));
    }

    @Transactional(readOnly = true)
    public PromotionPreviewResponse preview(String promotionCode, double subtotal) {
        AppliedPromotion promotion = calculate(findByCode(promotionCode), subtotal);
        return new PromotionPreviewResponse(promotion.code(), promotion.name(), money(subtotal),
                promotion.discountAmount(), money(subtotal - promotion.discountAmount()));
    }

    /** Locks and consumes a code. It must be called inside the checkout transaction. */
    @Transactional
    public AppliedPromotion applyForCheckout(String promotionCode, double subtotal) {
        if (isBlank(promotionCode)) return AppliedPromotion.none();
        Promotion promotion = promotionRepository.findByCodeForUpdate(normalizeCode(promotionCode))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Promotion code is invalid"));
        AppliedPromotion applied = calculate(promotion, subtotal);
        promotion.setUsageCount((promotion.getUsageCount() == null ? 0 : promotion.getUsageCount()) + 1);
        return applied;
    }

    private Promotion findByCode(String promotionCode) {
        if (isBlank(promotionCode)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a promotion code");
        }
        return promotionRepository.findByCodeIgnoreCase(normalizeCode(promotionCode))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Promotion code is invalid"));
    }

    private AppliedPromotion calculate(Promotion promotion, double subtotal) {
        if (!Boolean.TRUE.equals(promotion.getActive())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This promotion is not active");
        }
        // Promotion dates are entered by the Thailand-based admin workspace as
        // local date-times. Make the comparison explicit so it stays correct
        // even if Docker or a developer machine uses UTC.
        LocalDateTime now = LocalDateTime.now(PROMOTION_TIME_ZONE);
        if (promotion.getStartsAt().isAfter(now)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This promotion has not started yet");
        }
        if (promotion.getEndsAt().isBefore(now)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This promotion has expired");
        }
        int used = promotion.getUsageCount() == null ? 0 : promotion.getUsageCount();
        if (promotion.getUsageLimit() != null && used >= promotion.getUsageLimit()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This promotion has reached its usage limit");
        }
        double minimum = number(promotion.getMinimumOrderAmount());
        if (subtotal < minimum) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "This promotion requires a minimum order of " + money(minimum));
        }

        double discount = PERCENTAGE.equals(promotion.getDiscountType())
                ? subtotal * number(promotion.getDiscountValue()) / 100d
                : number(promotion.getDiscountValue());
        return new AppliedPromotion(promotion.getCode(), promotion.getName(), money(Math.min(subtotal, discount)));
    }

    private Promotion findPromotion(Integer promotionId) {
        return promotionRepository.findById(promotionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Promotion not found"));
    }

    private void applyRequest(Promotion promotion, PromotionRequest request, String code) {
        if (request == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Promotion data is required");
        String name = required(request.name(), "Promotion name is required");
        String type = normalizeType(request.discountType());
        double value = requiredNonNegative(request.discountValue(), "Discount value is required");
        double minimum = request.minimumOrderAmount() == null ? 0d
                : requiredNonNegative(request.minimumOrderAmount(), "Minimum order amount must be zero or more");
        if (value <= 0d) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Discount value must be greater than zero");
        if (PERCENTAGE.equals(type) && value > 100d) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Percentage discount cannot exceed 100");
        }
        if (request.usageLimit() != null && request.usageLimit() < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usage limit must be at least 1");
        }
        if (request.startsAt() == null || request.endsAt() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Promotion start and end dates are required");
        }
        if (!request.endsAt().isAfter(request.startsAt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Promotion end date must be after its start date");
        }
        promotion.setCode(code);
        promotion.setName(name);
        promotion.setDiscountType(type);
        promotion.setDiscountValue(money(value));
        promotion.setMinimumOrderAmount(money(minimum));
        promotion.setUsageLimit(request.usageLimit());
        promotion.setStartsAt(request.startsAt());
        promotion.setEndsAt(request.endsAt());
        promotion.setActive(request.active() == null || request.active());
        if (promotion.getUsageCount() == null) promotion.setUsageCount(0);
    }

    private PromotionResponse toResponse(Promotion promotion) {
        return new PromotionResponse(promotion.getPromotionId(), promotion.getCode(), promotion.getName(),
                promotion.getDiscountType(), promotion.getDiscountValue(), promotion.getMinimumOrderAmount(),
                promotion.getUsageLimit(), promotion.getUsageCount(), promotion.getStartsAt(), promotion.getEndsAt(),
                promotion.getActive());
    }

    private String normalizeCode(String value) {
        String code = required(value, "Promotion code is required").toUpperCase(Locale.ROOT);
        if (!code.matches("[A-Z0-9_-]{3,50}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Promotion code must use 3-50 uppercase letters, numbers, hyphens, or underscores");
        }
        return code;
    }

    private String normalizeType(String value) {
        String type = required(value, "Discount type is required").toUpperCase(Locale.ROOT);
        if (!PERCENTAGE.equals(type) && !FIXED_AMOUNT.equals(type)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Discount type must be PERCENTAGE or FIXED_AMOUNT");
        }
        return type;
    }

    private String required(String value, String message) {
        if (isBlank(value)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        return value.trim();
    }

    private double requiredNonNegative(Double value, String message) {
        if (value == null || !Double.isFinite(value) || value < 0d) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return value;
    }

    private boolean isBlank(String value) { return value == null || value.trim().isEmpty(); }
    private double number(Double value) { return value == null ? 0d : value; }
    private double money(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    public record AppliedPromotion(String code, String name, double discountAmount) {
        public static AppliedPromotion none() { return new AppliedPromotion(null, null, 0d); }
        public boolean isApplied() { return code != null && discountAmount > 0d; }
    }
}
