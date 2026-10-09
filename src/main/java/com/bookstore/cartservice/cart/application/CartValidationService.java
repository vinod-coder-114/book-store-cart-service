package com.bookstore.cartservice.cart.application;

import com.bookstore.cartservice.cart.domain.Cart;
import com.bookstore.cartservice.cart.domain.CartItem;
import com.bookstore.cartservice.cart.integration.catalog.CartRulesProperties;
import com.bookstore.cartservice.cart.integration.catalog.CatalogBook;
import com.bookstore.cartservice.cart.integration.catalog.CatalogBookNotFoundException;
import com.bookstore.cartservice.cart.integration.catalog.CatalogClient;
import com.bookstore.cartservice.cart.integration.catalog.CatalogResponseException;
import com.bookstore.cartservice.cart.integration.catalog.CatalogServiceUnavailableException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CartValidationService {

    static final String CODE_BOOK_NOT_FOUND = "BOOK_NOT_FOUND";
    static final String CODE_STOCK_UNAVAILABLE = "STOCK_UNAVAILABLE";
    static final String CODE_PRICE_CHANGED = "PRICE_CHANGED";
    static final String CODE_CATALOG_UNAVAILABLE = "CATALOG_UNAVAILABLE";
    static final String CODE_INVALID_CATALOG_RESPONSE = "INVALID_CATALOG_RESPONSE";

    private final CatalogClient catalogClient;
    private final CartRulesProperties cartRulesProperties;

    public CartValidationService(CatalogClient catalogClient, CartRulesProperties cartRulesProperties) {
        this.catalogClient = catalogClient;
        this.cartRulesProperties = cartRulesProperties;
    }

    public CatalogBook validateBookChange(String bookId, int requestedQuantity) {
        if (requestedQuantity <= 0) {
            throw new CartRuleViolationException("Quantity must be greater than zero");
        }
        if (requestedQuantity > cartRulesProperties.getMaxQuantityPerItem()) {
            throw new CartRuleViolationException("Quantity exceeds the configured maximum of "
                    + cartRulesProperties.getMaxQuantityPerItem());
        }

        CatalogBook catalogBook = catalogClient.getBook(bookId);
        if (catalogBook.stock() < requestedQuantity) {
            throw new CartRuleViolationException("Requested quantity exceeds catalog availability");
        }
        return catalogBook;
    }

    public CartValidationResult validateForCheckout(Cart cart) {
        List<CartValidationIssue> issues = new ArrayList<>();
        for (CartItem item : cart.getItems()) {
            validateItemForCheckout(item, issues);
        }
        return issues.isEmpty() ? CartValidationResult.success() : CartValidationResult.failure(issues);
    }

    private void validateItemForCheckout(CartItem item, List<CartValidationIssue> issues) {
        try {
            CatalogBook catalogBook = catalogClient.getBook(item.getBookId());
            if (catalogBook.stock() < item.getQuantity()) {
                issues.add(new CartValidationIssue(
                        item.getBookId(),
                        CODE_STOCK_UNAVAILABLE,
                        "Requested quantity is no longer available",
                        item.getQuantity(),
                        catalogBook.stock(),
                        item.getUnitPrice(),
                        catalogBook.unitPrice()
                ));
            }
            if (catalogBook.unitPrice().compareTo(item.getUnitPrice()) != 0) {
                issues.add(new CartValidationIssue(
                        item.getBookId(),
                        CODE_PRICE_CHANGED,
                        "Catalog price has changed since the item was added",
                        item.getQuantity(),
                        catalogBook.stock(),
                        item.getUnitPrice(),
                        catalogBook.unitPrice()
                ));
            }
        } catch (CatalogBookNotFoundException exception) {
            issues.add(new CartValidationIssue(
                    item.getBookId(),
                    CODE_BOOK_NOT_FOUND,
                    "Book no longer exists in the catalog",
                    item.getQuantity(),
                    null,
                    item.getUnitPrice(),
                    null
            ));
        } catch (CatalogResponseException exception) {
            issues.add(new CartValidationIssue(
                    item.getBookId(),
                    CODE_INVALID_CATALOG_RESPONSE,
                    exception.getMessage(),
                    item.getQuantity(),
                    null,
                    item.getUnitPrice(),
                    null
            ));
        } catch (CatalogServiceUnavailableException exception) {
            issues.add(new CartValidationIssue(
                    item.getBookId(),
                    CODE_CATALOG_UNAVAILABLE,
                    exception.getMessage(),
                    item.getQuantity(),
                    null,
                    item.getUnitPrice(),
                    null
            ));
        }
    }
}

