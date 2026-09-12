package org.example.dto;

public record CatalogProductResponse(
        Integer id,
        String sku,
        String name,
        String type,
        String game,
        Double price,
        Integer stock,
        String imageUrl,
        String description,
        String source,
        CatalogStoreResponse store
) {}
