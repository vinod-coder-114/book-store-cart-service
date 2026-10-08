package com.bookstore.cartservice.cart.persistence;

import com.bookstore.cartservice.cart.domain.Cart;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CartRepository extends JpaRepository<Cart, UUID> {

    @EntityGraph(attributePaths = "items")
    Optional<Cart> findByOpenUserId(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select distinct cart from Cart cart left join fetch cart.items where cart.openUserId = :userId")
    Optional<Cart> findOpenCartWithItemsForUpdate(@Param("userId") UUID userId);
}
