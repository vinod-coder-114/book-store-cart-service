package com.bookstore.cartservice.cart.application;

import com.bookstore.cartservice.cart.domain.Cart;
import com.bookstore.cartservice.cart.domain.CartItem;
import com.bookstore.cartservice.cart.domain.CartNotFoundException;
import com.bookstore.cartservice.cart.integration.catalog.CatalogBook;
import com.bookstore.cartservice.cart.persistence.CartRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class CartTransactionService {

    private static final Logger logger = LoggerFactory.getLogger(CartTransactionService.class);
    private final CartRepository cartRepository;
    private final CartValidationService cartValidationService;

    public CartTransactionService(CartRepository cartRepository, CartValidationService cartValidationService) {
        this.cartRepository = cartRepository;
        this.cartValidationService = cartValidationService;
    }

    @Transactional(readOnly = true)
    public Cart getOpenCart(UUID userId) {
        logger.info("Fetching open cart for userId: {}", userId);
        return cartRepository.findByOpenUserId(userId)
                .orElseThrow(CartNotFoundException::new);
    }

    @Transactional(readOnly = true)
    public Optional<Cart> findOpenCart(UUID userId) {
        logger.info("Finding open cart for userId: {}", userId);
        return cartRepository.findByOpenUserId(userId);
    }

    @Transactional
    public Cart getOrCreateOpenCart(UUID userId) {
        logger.info("Getting or creating open cart for userId: {}", userId);
        return cartRepository.findOpenCartWithItemsForUpdate(userId)
                .orElseGet(() -> cartRepository.save(Cart.createFor(userId)));
    }

    @Transactional
    public Cart addBook(UUID userId, String bookId, int quantity) {
        logger.info("Adding book with bookId: {} and quantity: {} to cart for userId: {}", bookId, quantity, userId);
        Cart cart = getOrCreateOpenCart(userId);
        int targetQuantity = Math.addExact(currentQuantity(cart, bookId), quantity);
        CatalogBook catalogBook = cartValidationService.validateBookChange(bookId, targetQuantity);
        cart.addBook(bookId, quantity, catalogBook.unitPrice());
        return cartRepository.save(cart);
    }

    @Transactional
    public Cart updateBookQuantity(UUID userId, String bookId, int quantity) {
        logger.info("Updating book quantity with bookId: {} and quantity: {} for userId: {}", bookId, quantity, userId);
        Cart cart = getCartForUpdate(userId);
        ensureItemPresent(cart, bookId);
        cartValidationService.validateBookChange(bookId, quantity);
        cart.updateBookQuantity(bookId, quantity);
        return cartRepository.save(cart);
    }

    @Transactional
    public Cart removeBook(UUID userId, String bookId) {
        logger.info("Removing book with bookId: {} from cart for userId: {}", bookId, userId);
        Cart cart = getCartForUpdate(userId);
        cart.removeBook(bookId);
        return cartRepository.save(cart);
    }

    @Transactional
    public Cart clear(UUID userId) {
        logger.info("Clearing cart for userId: {}", userId);
        Cart cart = getCartForUpdate(userId);
        cart.clear();
        return cartRepository.save(cart);
    }

    @Transactional
    public Cart prepareCheckout(UUID userId) {
        logger.info("Preparing checkout for userId: {}", userId);
        Cart cart = getCartForUpdate(userId);
        CartValidationResult validationResult = cartValidationService.validateForCheckout(cart);
        if (!validationResult.isValid()) {
            throw new CartCheckoutValidationException(validationResult);
        }
        cart.prepareCheckout();
        return cartRepository.save(cart);
    }

    @Transactional(readOnly = true)
    public CartValidationResult validateOpenCart(UUID userId) {
        Cart cart = getOpenCart(userId);
        return cartValidationService.validateForCheckout(cart);
    }

    @Transactional
    public Cart completeCheckout(UUID userId) {
        Cart cart = getCartForUpdate(userId);
        cart.completeCheckout();
        return cartRepository.save(cart);
    }

    @Transactional
    public Cart cancelCheckout(UUID userId) {
        Cart cart = getCartForUpdate(userId);
        cart.cancelCheckout();
        return cartRepository.save(cart);
    }

    private Cart getCartForUpdate(UUID userId) {
        return cartRepository.findOpenCartWithItemsForUpdate(userId)
                .orElseThrow(CartNotFoundException::new);
    }

    private static int currentQuantity(Cart cart, String bookId) {
        return cart.getItems().stream()
                .filter(item -> item.getBookId().equals(bookId))
                .mapToInt(CartItem::getQuantity)
                .findFirst()
                .orElse(0);
    }

    private static void ensureItemPresent(Cart cart, String bookId) {
        boolean present = cart.getItems().stream()
                .map(CartItem::getBookId)
                .anyMatch(bookId::equals);
        if (!present) {
            throw new IllegalArgumentException("Book is not in the cart");
        }
    }
}
