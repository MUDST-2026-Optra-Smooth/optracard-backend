package org.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** Public shop data displayed when a buyer opens a marketplace seller profile. */
@Schema(description = "Public storefront profile and approved product inventory")
public record MarketplaceStoreProfileResponse(
        @Schema(description = "Store identifier", example = "5")
        Integer id,

        @Schema(description = "Store display name", example = "Siam Card Arena")
        String name,

        @Schema(description = "Store URL slug", example = "siam-card-arena")
        String slug,

        @Schema(description = "Store description", example = "Premier local card shop specializing in One Piece and Pokemon TCG.")
        String description,

        @Schema(description = "Store physical location / city", example = "Pathum Wan, Bangkok")
        String location,

        @Schema(description = "Store logo / banner image URL", example = "https://images.optracard.com/stores/siamcard.png")
        String profileImage,

        @Schema(description = "Publicly listed and approved products offered by this merchant")
        List<CatalogProductResponse> products
) {}
