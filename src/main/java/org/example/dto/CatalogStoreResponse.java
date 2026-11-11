package org.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Seller store summary for catalog listings")
public record CatalogStoreResponse(
        @Schema(description = "Store ID", example = "3")
        Integer id,

        @Schema(description = "Store display name", example = "Kanto Card Shop")
        String name,

        @Schema(description = "Store URL slug", example = "kanto-cards")
        String slug
) {}
