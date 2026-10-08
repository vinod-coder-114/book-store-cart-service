package com.bookstore.cartservice.cart.application;

import java.util.List;

public record CartValidationResult(boolean valid, List<CartValidationIssue> issues) {

    public CartValidationResult {
        issues = List.copyOf(issues);
    }

    public static CartValidationResult success() {
        return new CartValidationResult(true, List.of());
    }

    public static CartValidationResult failure(List<CartValidationIssue> issues) {
        if (issues.isEmpty()) {
            throw new IllegalArgumentException("issues must not be empty for an invalid result");
        }
        return new CartValidationResult(false, issues);
    }

    public boolean isValid() {
        return valid;
    }
}

