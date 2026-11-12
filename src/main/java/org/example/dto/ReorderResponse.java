package org.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Result of putting a past order's items back into the buyer's cart.
 * <p>
 * {@code added} lists every line that reached the cart. A line whose
 * {@code addedQuantity} is lower than {@code requestedQuantity} was limited by
 * stock. {@code skipped} lists lines that could not be added at all, so the
 * buyer is always told what did not make it.
 */
@Schema(description = "Result of re-adding items from a previous order into the active cart")
public record ReorderResponse(
        @Schema(description = "Source order number", example = "ORD-20261111-9871")
        String orderNumber,

        @Schema(description = "Count of lines successfully placed in cart", example = "2")
        int addedCount,

        @Schema(description = "Count of lines that could not be added", example = "0")
        int skippedCount,

        @Schema(description = "Items added to cart")
        List<Line> added,

        @Schema(description = "Items skipped due to unavailability or insufficient inventory")
        List<Line> skipped
) {
    /** Reason codes for lines that are skipped or only partly added. */
    public static final String UNAVAILABLE = "UNAVAILABLE";
    public static final String OUT_OF_STOCK = "OUT_OF_STOCK";
    public static final String LIMITED_STOCK = "LIMITED_STOCK";

    @Schema(description = "Reorder line item detail")
    public record Line(
            @Schema(description = "Product ID", example = "42")
            Integer productId,

            @Schema(description = "Product name", example = "Charizard VMAX Shiny")
            String name,

            @Schema(description = "Quantity requested from historical order", example = "2")
            Integer requestedQuantity,

            @Schema(description = "Quantity actually added to cart", example = "2")
            Integer addedQuantity,

            @Schema(description = "Reason code if partially or fully skipped (UNAVAILABLE, OUT_OF_STOCK, LIMITED_STOCK)", example = "LIMITED_STOCK")
            String reason,

            @Schema(description = "Explanation message", example = "Only 2 copies remaining in stock.")
            String message
    ) {}
}
