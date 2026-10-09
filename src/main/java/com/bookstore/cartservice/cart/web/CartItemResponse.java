package com.bookstore.cartservice.cart.web;

import com.bookstore.cartservice.cart.domain.CartItem;

import java.math.BigDecimal;

public record CartItemResponse(
        String bookId,
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

