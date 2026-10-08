package com.bookstore.cartservice.cart.web;

public record CartCountResponse(
        int totalUnits,
        int distinctItemCount
) {
    public static CartCountResponse empty() {
        return new CartCountResponse(0, 0);
    }
}

