package org.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Available facet options for catalog filtering")
public record CatalogFilterOptionsResponse(
        @Schema(description = "Available card types", example = "[\"Single\", \"Booster Pack\", \"Booster Box\", \"Accessories\"]")
        List<String> types,

        @Schema(description = "Available card games")
        List<GameOption> games,

        @Schema(description = "Available card languages", example = "[\"English\", \"Japanese\", \"Thai\"]")
        List<String> languages,

        @Schema(description = "Available listing origins", example = "[\"OFFICIAL\", \"MARKETPLACE\"]")
        List<String> sources,

        @Schema(description = "Available merchant stores")
        List<StoreOption> stores,

        @Schema(description = "Min and max price boundaries across available inventory")
        PriceRange price
) {
    @Schema(description = "Game filter option")
    public record GameOption(
            @Schema(description = "Game ID", example = "1")
            Integer id,
            @Schema(description = "Game title", example = "Pokemon")
            String name
    ) {}

    @Schema(description = "Store filter option")
    public record StoreOption(
            @Schema(description = "Store ID", example = "2")
            Integer id,
            @Schema(description = "Store name", example = "Kanto Card Shop")
            String name
    ) {}

    @Schema(description = "Price range boundaries")
    public record PriceRange(
            @Schema(description = "Minimum price", example = "10.00")
            Double min,
            @Schema(description = "Maximum price", example = "50000.00")
            Double max
    ) {}
}
