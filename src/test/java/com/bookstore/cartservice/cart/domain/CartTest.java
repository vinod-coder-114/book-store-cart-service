package com.bookstore.cartservice.cart.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CartTest {

    private final UUID userId = UUID.randomUUID();
    private final String bookId = UUID.randomUUID().toString();

    @Test
    void addingAnExistingBookIncrementsQuantityAndPreservesPriceSnapshot() {
        Cart cart = Cart.createFor(userId);
        cart.addBook(bookId, 2, new BigDecimal("12.50"));

        CartItem item = cart.addBook(bookId, 3, new BigDecimal("14.00"));

        assertEquals(1, cart.getItems().size());
        assertEquals(5, item.getQuantity());
        assertEquals(new BigDecimal("12.50"), item.getUnitPrice());
    }

    @Test
    void rejectsInvalidQuantityAndPrice() {
        Cart cart = Cart.createFor(userId);

        assertThrows(IllegalArgumentException.class,
                () -> cart.addBook(bookId, 0, new BigDecimal("12.50")));
        assertThrows(IllegalArgumentException.class,
                () -> cart.addBook(bookId, 1, new BigDecimal("1.001")));
        assertThrows(IllegalArgumentException.class,
                () -> cart.addBook(bookId, 1, new BigDecimal("-0.01")));
    }

    @Test
    void itemCanBeUpdatedRemovedAndCartCleared() {
        Cart cart = Cart.createFor(userId);
        cart.addBook(bookId, 2, new BigDecimal("12.50"));
        cart.updateBookQuantity(bookId, 4);
        assertEquals(4, cart.getItems().getFirst().getQuantity());

        assertTrue(cart.removeBook(bookId));
        assertFalse(cart.removeBook(bookId));
        cart.addBook(bookId, 1, new BigDecimal("12.50"));
        cart.clear();

        assertTrue(cart.getItems().isEmpty());
    }

    @Test
    void subtotalAndCountSemanticsAreDerivedFromStoredSnapshots() {
        Cart cart = Cart.createFor(userId);
        String secondBookId = UUID.randomUUID().toString();
        cart.addBook(bookId, 2, new BigDecimal("12.50"));
        cart.addBook(secondBookId, 1, new BigDecimal("7.25"));

        assertEquals(3, cart.getTotalUnits());
        assertEquals(2, cart.getDistinctItemCount());
        assertEquals(new BigDecimal("32.25"), cart.getSubtotal());
    }

    @Test
    void checkoutLifecyclePreventsMutationsUntilCancelledOrCompleted() {
        Cart cart = Cart.createFor(userId);
        cart.addBook(bookId, 1, new BigDecimal("12.50"));
        cart.prepareCheckout();

        assertEquals(CartStatus.CHECKOUT_PENDING, cart.getStatus());
        assertThrows(IllegalStateException.class,
                () -> cart.updateBookQuantity(bookId, 2));

        cart.cancelCheckout();
        cart.updateBookQuantity(bookId, 2);
        cart.prepareCheckout();
        cart.completeCheckout();

        assertEquals(CartStatus.COMPLETED, cart.getStatus());
        assertThrows(IllegalStateException.class,
                () -> cart.removeBook(bookId));
    }

    @Test
    void cannotPrepareAnEmptyCartForCheckout() {
        Cart cart = Cart.createFor(userId);

        assertThrows(IllegalStateException.class, cart::prepareCheckout);
    }
}
