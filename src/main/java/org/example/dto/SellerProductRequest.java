package org.example.dto;

/** Product data submitted by an approved marketplace seller. */
public record SellerProductRequest(
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
        Integer templateProductId
) {}
