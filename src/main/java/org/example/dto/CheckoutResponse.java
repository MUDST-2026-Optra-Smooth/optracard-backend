package org.example.dto;

import java.util.List;

/** Result of checkout. A cart with multiple sellers creates one order for each seller. */
public record CheckoutResponse(
        List<OrderResponse> orders,
        Integer orderCount,
        Double total
) {}
