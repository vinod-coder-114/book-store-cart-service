package com.bookstore.cartservice.cart.web;

public class InvalidAuthenticationClaimException extends RuntimeException {

    public InvalidAuthenticationClaimException(String message) {
        super(message);
    }
}

