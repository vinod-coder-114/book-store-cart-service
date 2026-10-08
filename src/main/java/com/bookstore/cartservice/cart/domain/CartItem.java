package com.bookstore.cartservice.cart.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "cart_items",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_cart_items_cart_book",
                columnNames = {"cart_id", "book_id"}
        )
)
public class CartItem {

    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, columnDefinition = "binary(16)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_id", nullable = false, columnDefinition = "binary(16)")
    private Cart cart;

    @Getter
    @Column(name = "book_id", nullable = false, columnDefinition = "binary(16)")
    private UUID bookId;

    @Getter
    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Getter
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Getter
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Getter
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CartItem() {
    }

    CartItem(Cart cart, UUID bookId, int quantity, BigDecimal unitPrice) {
        this.cart = Objects.requireNonNull(cart, "cart must not be null");
        this.bookId = Objects.requireNonNull(bookId, "bookId must not be null");
        this.quantity = quantity;
        this.unitPrice = normalizePrice(unitPrice);
    }

    public BigDecimal getLineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    void detach() {
        cart = null;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    static BigDecimal normalizePrice(BigDecimal price) {
        Objects.requireNonNull(price, "unitPrice must not be null");
        if (price.signum() < 0) {
            throw new IllegalArgumentException("Unit price must not be negative");
        }

        BigDecimal normalized;
        try {
            normalized = price.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Unit price must have at most two decimal places", exception);
        }
        if (normalized.precision() > 10) {
            throw new IllegalArgumentException("Unit price exceeds the supported precision");
        }
        return normalized;
    }
}
