package org.example.dto;

public record CollectionItemResponse(
        Integer id,
        String name,
        String game,
        String imageUrl,
        Long quantity
) {}
