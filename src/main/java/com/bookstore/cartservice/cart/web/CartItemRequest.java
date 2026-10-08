package com.bookstore.cartservice.cart.web;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CartItemRequest(
        @NotNull(message = "bookId is required")
        UUID bookId,
        @Min(value = 1, message = "quantity must be greater than zero")
        int quantity
) {
}

