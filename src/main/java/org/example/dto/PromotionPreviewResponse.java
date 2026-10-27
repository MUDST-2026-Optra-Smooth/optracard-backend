package org.example.dto;

public record PromotionPreviewResponse(
        String code,
        String name,
        Double subtotal,
        Double discountAmount,
        Double discountedSubtotal
) {}
