package com.bookstore.cartservice.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.time.Instant;

@Entity(name = "users")
public record User(
        @Id
        String user_id,
        @Column(nullable = false)
        String email,
        Instant created_at,
        Instant updated_at
) {
}
