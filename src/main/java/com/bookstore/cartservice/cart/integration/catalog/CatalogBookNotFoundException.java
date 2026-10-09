package com.bookstore.cartservice.cart.integration.catalog;


public class CatalogBookNotFoundException extends CatalogIntegrationException {

    public CatalogBookNotFoundException(String bookId) {
        super("Catalog book not found: " + bookId);
    }

    public CatalogBookNotFoundException(String bookId, Throwable cause) {
        super("Catalog book not found: " + bookId, cause);
    }
}

