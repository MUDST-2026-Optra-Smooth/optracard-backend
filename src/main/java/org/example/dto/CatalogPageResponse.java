package org.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Paginated product catalog response")
public record CatalogPageResponse(
        @Schema(description = "List of products on current page")
        List<CatalogProductResponse> items,

        @Schema(description = "Current page index (0-based)", example = "0")
        int page,

        @Schema(description = "Requested page size", example = "20")
        int pageSize,

        @Schema(description = "Total number of products matching criteria", example = "142")
        long totalItems,

        @Schema(description = "Total number of available pages", example = "8")
        int totalPages,

        @Schema(description = "Active sort order", example = "price,asc")
        String sort
) {}
