package org.example.dto;

public record OrderItemResponse(
        Integer productId,
        String name,
        String game,
        String imageUrl,
        Double price,
        Integer quantity,
        String storeName
) {}
