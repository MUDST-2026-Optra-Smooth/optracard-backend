package org.example.dto;

import java.util.List;

/**
 * Result of putting a past order's items back into the buyer's cart.
 * <p>
 * {@code added} lists every line that reached the cart. A line whose
 * {@code addedQuantity} is lower than {@code requestedQuantity} was limited by
 * stock. {@code skipped} lists lines that could not be added at all, so the
 * buyer is always told what did not make it.
 */
public record ReorderResponse(
        String orderNumber,
        int addedCount,
        int skippedCount,
        List<Line> added,
        List<Line> skipped
) {
    /** Reason codes for lines that are skipped or only partly added. */
    public static final String UNAVAILABLE = "UNAVAILABLE";
    public static final String OUT_OF_STOCK = "OUT_OF_STOCK";
    public static final String LIMITED_STOCK = "LIMITED_STOCK";

    public record Line(
            Integer productId,
            String name,
            Integer requestedQuantity,
            Integer addedQuantity,
            String reason,
            String message
    ) {}
}
