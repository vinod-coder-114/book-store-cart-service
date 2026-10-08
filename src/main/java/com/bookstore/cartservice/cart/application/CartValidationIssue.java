package com.bookstore.cartservice.cart.application;

import java.math.BigDecimal;
import java.util.UUID;

public record CartValidationIssue(
        UUID bookId,
        String code,
        String message,
        Integer requestedQuantity,
        Long availableStock,
        BigDecimal cartUnitPrice,
        BigDecimal currentUnitPrice
) {
}

