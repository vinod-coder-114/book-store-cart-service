package com.bookstore.cartservice.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.time.Instant;

@Entity(name = "carts")
public record Cart(
        @Id
        String cart_id,
        @Column(nullable = false)
        String user_id,
        @Column(nullable = false)
        Double price,
        Instant created_at,
        Instant updated_at
) {
}
