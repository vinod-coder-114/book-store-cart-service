package com.bookstore.cartservice.cart.application;

import com.bookstore.cartservice.cart.domain.Cart;
import com.bookstore.cartservice.cart.integration.catalog.CartRulesProperties;
import com.bookstore.cartservice.cart.integration.catalog.CatalogBook;
import com.bookstore.cartservice.cart.integration.catalog.CatalogBookNotFoundException;
import com.bookstore.cartservice.cart.integration.catalog.CatalogClient;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CartValidationServiceTest {

    @Test
    void validateBookChangeRejectsConfiguredMaximum() {
        String bookId = UUID.randomUUID().toString();
        CartValidationService service = new CartValidationService(
                new StubCatalogClient(Map.of(bookId, new CatalogBook(bookId, new BigDecimal("12.50"), "INR", 10))),
                rulesWithMax(5)
        );

        CartRuleViolationException exception = assertThrows(CartRuleViolationException.class,
                () -> service.validateBookChange(bookId, 6));

        assertTrue(exception.getMessage().contains("configured maximum"));
    }

    @Test
    void validateBookChangeRejectsOutOfStockRequests() {
        String bookId = UUID.randomUUID().toString();
        CartValidationService service = new CartValidationService(
                new StubCatalogClient(Map.of(bookId, new CatalogBook(bookId, new BigDecimal("12.50"), "INR", 2))),
                rulesWithMax(10)
        );

        CartRuleViolationException exception = assertThrows(CartRuleViolationException.class,
                () -> service.validateBookChange(bookId, 3));

        assertTrue(exception.getMessage().contains("availability"));
    }

    @Test
    void validateForCheckoutReturnsValidWhenCatalogStateStillMatchesCart() {
        UUID userId = UUID.randomUUID();
        String bookId = UUID.randomUUID().toString();
        Cart cart = Cart.createFor(userId);
        cart.addBook(bookId, 2, new BigDecimal("12.50"));

        CartValidationService service = new CartValidationService(
                new StubCatalogClient(Map.of(bookId, new CatalogBook(bookId, new BigDecimal("12.50"), "INR", 5))),
                rulesWithMax(10)
        );

        CartValidationResult result = service.validateForCheckout(cart);

        assertTrue(result.isValid());
        assertTrue(result.issues().isEmpty());
    }

    @Test
    void validateForCheckoutReturnsStructuredIssuesForChangedCatalogState() {
        UUID userId = UUID.randomUUID();
        String priceAndStockBookId = UUID.randomUUID().toString();
        String missingBookId = UUID.randomUUID().toString();
        Cart cart = Cart.createFor(userId);
        cart.addBook(priceAndStockBookId, 2, new BigDecimal("12.50"));
        cart.addBook(missingBookId, 1, new BigDecimal("5.00"));

        StubCatalogClient catalogClient = new StubCatalogClient(Map.of(
                priceAndStockBookId, new CatalogBook(priceAndStockBookId, new BigDecimal("13.00"), "INR", 1)
        ));
        catalogClient.failures.put(missingBookId, new CatalogBookNotFoundException(missingBookId));

        CartValidationService service = new CartValidationService(catalogClient, rulesWithMax(10));

        CartValidationResult result = service.validateForCheckout(cart);

        assertFalse(result.isValid());
        assertEquals(3, result.issues().size());
        assertEquals(Set.of(
                        CartValidationService.CODE_STOCK_UNAVAILABLE,
                        CartValidationService.CODE_PRICE_CHANGED,
                        CartValidationService.CODE_BOOK_NOT_FOUND
                ),
                result.issues().stream().map(CartValidationIssue::code).collect(java.util.stream.Collectors.toSet()));
    }

    private static CartRulesProperties rulesWithMax(int maxQuantityPerItem) {
        CartRulesProperties properties = new CartRulesProperties();
        properties.setMaxQuantityPerItem(maxQuantityPerItem);
        return properties;
    }

    private static final class StubCatalogClient implements CatalogClient {

        private final Map<String, CatalogBook> books;
        private final Map<String, RuntimeException> failures = new HashMap<>();

        private StubCatalogClient(Map<String, CatalogBook> books) {
            this.books = books;
        }

        @Override
        public CatalogBook getBook(String bookId) {
            RuntimeException failure = failures.get(bookId);
            if (failure != null) {
                throw failure;
            }
            CatalogBook book = books.get(bookId);
            if (book == null) {
                throw new CatalogBookNotFoundException(bookId);
            }
            return book;
        }
    }
}

