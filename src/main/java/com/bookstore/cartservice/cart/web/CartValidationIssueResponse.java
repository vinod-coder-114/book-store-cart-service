package com.bookstore.cartservice.cart.web;

import com.bookstore.cartservice.cart.application.CartValidationIssue;

import java.math.BigDecimal;

public record CartValidationIssueResponse(
        String bookId,
        String code,
        String message,
        Integer requestedQuantity,
        Long availableStock,
        BigDecimal cartUnitPrice,
        BigDecimal currentUnitPrice
) {
    static CartValidationIssueResponse from(CartValidationIssue issue) {
        return new CartValidationIssueResponse(
                issue.bookId(),
                issue.code(),
                issue.message(),
                issue.requestedQuantity(),
                issue.availableStock(),
                issue.cartUnitPrice(),
                issue.currentUnitPrice()
        );
    }
}

