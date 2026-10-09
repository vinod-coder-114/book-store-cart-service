package com.bookstore.cartservice.cart.web;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;


public record CartItemRequest(
        @NotBlank(message = "bookId is required")
        String bookId,
        @Min(value = 1, message = "quantity must be greater than zero")
        int quantity
) {
}

