package org.example.dto;

public record SellerProductResponse(
        Integer id,
        String name,
        String game,
        String type,
        Double cost,
        Double price,
        Integer stock,
        String imageUrl,
        String productSet,
        String language,
        String description,
        String approvalStatus,
        Boolean active,
        Integer storeId,
        String storeName
) {}
