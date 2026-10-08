package com.bookstore.cartservice.cart.application;

public class CartRuleViolationException extends RuntimeException {

    public CartRuleViolationException(String message) {
        super(message);
    }
}

