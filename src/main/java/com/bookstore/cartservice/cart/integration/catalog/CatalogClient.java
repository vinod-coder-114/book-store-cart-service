package com.bookstore.cartservice.cart.integration.catalog;

import java.util.UUID;

public interface CatalogClient {

    CatalogBook getBook(UUID bookId);
}

