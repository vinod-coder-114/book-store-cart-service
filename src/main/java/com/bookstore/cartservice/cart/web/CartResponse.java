package com.bookstore.cartservice.cart.web;

import com.bookstore.cartservice.cart.domain.Cart;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CartResponse(
        UUID cartId,
        String status,
        List<CartItemResponse> items,
        int totalUnits,
        int distinctItemCount,
        BigDecimal subtotal,
        Instant createdAt,
        Instant updatedAt
) {
    static CartResponse from(Cart cart) {
        return new CartResponse(
                cart.getId(),
                cart.getStatus().name(),
                cart.getItems().stream().map(CartItemResponse::from).toList(),
                cart.getTotalUnits(),
                cart.getDistinctItemCount(),
                cart.getSubtotal(),
                cart.getCreatedAt(),
                cart.getUpdatedAt()
        );
    }
}

