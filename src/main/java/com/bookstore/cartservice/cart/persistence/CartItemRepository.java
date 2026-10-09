package com.bookstore.cartservice.cart.persistence;

import com.bookstore.cartservice.cart.domain.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

    Optional<CartItem> findByCart_IdAndBookId(UUID cartId, String bookId);
}
