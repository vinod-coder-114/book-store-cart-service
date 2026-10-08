package com.bookstore.cartservice.cart.integration.catalog;

import java.util.UUID;

public class CatalogBookNotFoundException extends CatalogIntegrationException {

    public CatalogBookNotFoundException(UUID bookId) {
        super("Catalog book not found: " + bookId);
    }

    public CatalogBookNotFoundException(UUID bookId, Throwable cause) {
        super("Catalog book not found: " + bookId, cause);
    }
}

