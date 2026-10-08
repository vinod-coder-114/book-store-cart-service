package com.bookstore.cartservice.cart.web;

import jakarta.validation.constraints.Min;

public record CartQuantityUpdateRequest(
        @Min(value = 1, message = "quantity must be greater than zero")
        int quantity
) {
}

