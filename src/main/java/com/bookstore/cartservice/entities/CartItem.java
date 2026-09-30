package com.bookstore.cartservice.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.time.Instant;

@Entity(name = "cart_items")
public record CartItem(
        @Id
        String cart_item_id,
        @Column(nullable = false)
        String cart_id,
        @Column(nullable = false)
        String product_id,
        @Column(nullable = false)
        int quantity,
        Instant created_at,
        Instant updated_at
) {
}
