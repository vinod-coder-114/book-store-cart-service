package com.bookstore.cartservice.cart.integration.catalog;

import java.math.BigDecimal;
import java.util.Objects;

public record CatalogBook(
        String id,
        BigDecimal unitPrice,
        String currency,
        long stock
) {
    public CatalogBook {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(unitPrice, "unitPrice must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
        if (stock < 0) {
            throw new IllegalArgumentException("stock must not be negative");
        }
    }
}

