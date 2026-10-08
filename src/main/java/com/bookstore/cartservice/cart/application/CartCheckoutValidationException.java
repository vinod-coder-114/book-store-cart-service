package com.bookstore.cartservice.cart.application;

public class CartCheckoutValidationException extends RuntimeException {

    private final CartValidationResult validationResult;

    public CartCheckoutValidationException(CartValidationResult validationResult) {
        super("Cart checkout validation failed");
        this.validationResult = validationResult;
    }

    public CartValidationResult getValidationResult() {
        return validationResult;
    }
}

