package com.bookstore.cartservice.cart.integration.catalog;

public class CatalogIntegrationException extends RuntimeException {

    public CatalogIntegrationException(String message) {
        super(message);
    }

    public CatalogIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}

