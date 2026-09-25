package org.example.dto;

import java.util.List;

/** Public shop data displayed when a buyer opens a marketplace seller profile. */
public record MarketplaceStoreProfileResponse(
        Integer id,
        String name,
        String slug,
        String description,
        String location,
        String profileImage,
        List<CatalogProductResponse> products
) {}
