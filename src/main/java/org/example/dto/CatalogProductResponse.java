package org.example.dto;

public record CatalogProductResponse(
        Integer id,
        String name,
        String type,
        String game,
        Double price,
        Integer stock,
        String imageUrl,
        String description,
        String productSet,
        String language,
        String source,
        CatalogStoreResponse store
) {}
