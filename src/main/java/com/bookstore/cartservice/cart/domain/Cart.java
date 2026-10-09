package com.bookstore.cartservice.cart.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "carts",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_carts_open_user",
                columnNames = "open_user_id"
        ),
        indexes = @Index(name = "idx_carts_user_id", columnList = "user_id")
)
public class Cart {

    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, columnDefinition = "binary(16)")
    private UUID id;

    @Getter
    @Column(name = "user_id", nullable = false, columnDefinition = "binary(16)")
    private UUID userId;

    @Column(name = "open_user_id", columnDefinition = "binary(16)")
    private UUID openUserId;

    @Getter
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private CartStatus status;

    @Getter
    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Getter
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Getter
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();

    protected Cart() {
    }

    private Cart(UUID userId) {
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.status = CartStatus.ACTIVE;
    }

    public static Cart createFor(UUID userId) {
        return new Cart(userId);
    }

    public CartItem addBook(String bookId, int quantity, BigDecimal unitPrice) {
        requireActive();
        Objects.requireNonNull(bookId, "bookId must not be null");
        validateQuantity(quantity);
        var priceSnapshot = CartItem.normalizePrice(unitPrice);

        for (var item : items) {
            if (item.getBookId().equals(bookId)) {
                item.setQuantity(Math.addExact(item.getQuantity(), quantity));
                markUpdated();
                return item;
            }
        }

        var item = new CartItem(this, bookId, quantity, priceSnapshot);
        items.add(item);
        markUpdated();
        return item;
    }

    public void updateBookQuantity(String bookId, int quantity) {
        requireActive();
        Objects.requireNonNull(bookId, "bookId must not be null");
        validateQuantity(quantity);

        findItem(bookId).setQuantity(quantity);
        markUpdated();
    }

    public boolean removeBook(String bookId) {
        requireActive();
        Objects.requireNonNull(bookId, "bookId must not be null");

        Iterator<CartItem> iterator = items.iterator();
        while (iterator.hasNext()) {
            CartItem item = iterator.next();
            if (item.getBookId().equals(bookId)) {
                iterator.remove();
                item.detach();
                markUpdated();
                return true;
            }
        }
        return false;
    }

    public void clear() {
        requireActive();
        for (CartItem item : items) {
            item.detach();
        }
        items.clear();
        markUpdated();
    }

    public void prepareCheckout() {
        requireActive();
        if (items.isEmpty()) {
            throw new IllegalStateException("An empty cart cannot be checked out");
        }
        status = CartStatus.CHECKOUT_PENDING;
    }

    public void completeCheckout() {
        if (status != CartStatus.CHECKOUT_PENDING) {
            throw new IllegalStateException("Only a checkout-pending cart can be completed");
        }
        status = CartStatus.COMPLETED;
    }

    public void cancelCheckout() {
        if (status != CartStatus.CHECKOUT_PENDING) {
            throw new IllegalStateException("Only a checkout-pending cart can be reopened");
        }
        status = CartStatus.ACTIVE;
    }

    public List<CartItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public int getTotalUnits() {
        return items.stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
    }

    public int getDistinctItemCount() {
        return items.size();
    }

    public BigDecimal getSubtotal() {
        return items.stream()
                .map(CartItem::getLineTotal)
                .reduce(new BigDecimal("0.00"), BigDecimal::add);
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        synchronizeOpenUserId();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
        synchronizeOpenUserId();
    }

    private void synchronizeOpenUserId() {
        openUserId = status == CartStatus.COMPLETED ? null : userId;
    }

    private void markUpdated() {
        updatedAt = Instant.now();
    }

    private CartItem findItem(String bookId) {
        return items.stream()
                .filter(item -> item.getBookId().equals(bookId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Book is not in the cart"));
    }

    private void requireActive() {
        if (status != CartStatus.ACTIVE) {
            throw new IllegalStateException("Only an active cart can be modified");
        }
    }

    private static void validateQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
    }
}
