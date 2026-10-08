package com.bookstore.cartservice.cart.domain;

public class CartNotFoundException extends RuntimeException {

    public CartNotFoundException() {
        super("No active cart exists for this user");
    }
}
