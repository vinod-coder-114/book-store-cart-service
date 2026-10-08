package com.bookstore.cartservice.cart.web;

import com.bookstore.cartservice.cart.domain.CartItem;

import java.math.BigDecimal;
import java.util.UUID;

public record CartItemResponse(
        UUID bookId,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {
    static CartItemResponse from(CartItem item) {
        return new CartItemResponse(
                item.getBookId(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getLineTotal()
        );
    }
}

