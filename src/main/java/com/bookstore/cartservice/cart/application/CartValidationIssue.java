package com.bookstore.cartservice.cart.application;

import java.math.BigDecimal;

public record CartValidationIssue(
        String bookId,
        String code,
        String message,
        Integer requestedQuantity,
        Long availableStock,
        BigDecimal cartUnitPrice,
        BigDecimal currentUnitPrice
) {
}

