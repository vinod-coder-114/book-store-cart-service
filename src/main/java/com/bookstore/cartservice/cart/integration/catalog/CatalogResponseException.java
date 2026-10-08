package com.bookstore.cartservice.cart.integration.catalog;

public class CatalogResponseException extends CatalogIntegrationException {

    public CatalogResponseException(String message) {
        super(message);
    }

    public CatalogResponseException(String message, Throwable cause) {
        super(message, cause);
    }
}

