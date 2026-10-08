package com.bookstore.cartservice.cart.web;

import com.bookstore.cartservice.cart.application.CartValidationResult;
import com.bookstore.cartservice.cart.domain.Cart;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CartValidationResponse(
        UUID cartId,
        boolean valid,
        int totalUnits,
        int distinctItemCount,
        BigDecimal subtotal,
        List<CartValidationIssueResponse> issues
) {
    static CartValidationResponse from(Cart cart, CartValidationResult validationResult) {
        return new CartValidationResponse(
                cart.getId(),
                validationResult.isValid(),
                cart.getTotalUnits(),
                cart.getDistinctItemCount(),
                cart.getSubtotal(),
                validationResult.issues().stream().map(CartValidationIssueResponse::from).toList()
        );
    }
}

