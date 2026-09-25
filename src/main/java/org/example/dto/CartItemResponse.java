package org.example.dto;

public record CartItemResponse(
        Integer productId,
        Integer quantity,
        String name,
        String game,
        String category,
        Double price,
        Integer stock,
        String imageUrl,
        Integer storeId,
        String storeName,
        String source
) {}
