package org.example.dto;

import java.util.List;

public record CartResponse(
        Integer cartId,
        List<CartItemResponse> items,
        Double subtotal,
        Integer totalItemCount
) {}
