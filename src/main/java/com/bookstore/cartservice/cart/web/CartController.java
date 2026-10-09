package com.bookstore.cartservice.cart.web;

import com.bookstore.cartservice.cart.application.CartTransactionService;
import com.bookstore.cartservice.cart.application.CartValidationResult;
import com.bookstore.cartservice.cart.domain.Cart;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/carts")
@Validated
public class CartController {

    private final CartTransactionService cartTransactionService;
    private final AuthenticatedUserIdResolver authenticatedUserIdResolver;

    public CartController(CartTransactionService cartTransactionService,
                          AuthenticatedUserIdResolver authenticatedUserIdResolver) {
        this.cartTransactionService = cartTransactionService;
        this.authenticatedUserIdResolver = authenticatedUserIdResolver;
    }

    @GetMapping
    public ResponseEntity<CartResponse> getCurrentCart(@AuthenticationPrincipal Jwt jwt) {
        Cart cart = cartTransactionService.getOpenCart(currentUserId(jwt));
        return ResponseEntity.ok(CartResponse.from(cart));
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItem(@AuthenticationPrincipal Jwt jwt,
                                                @Valid @RequestBody CartItemRequest request) {
        Cart cart = cartTransactionService.addBook(currentUserId(jwt), request.bookId(), request.quantity());
        return ResponseEntity.status(HttpStatus.CREATED).body(CartResponse.from(cart));
    }

    @PutMapping("/items/{bookId}")
    public ResponseEntity<CartResponse> updateItemQuantity(@AuthenticationPrincipal Jwt jwt,
                                                           @PathVariable String bookId,
                                                           @Valid @RequestBody CartQuantityUpdateRequest request) {
        Cart cart = cartTransactionService.updateBookQuantity(currentUserId(jwt), bookId, request.quantity());
        return ResponseEntity.ok(CartResponse.from(cart));
    }

    @DeleteMapping("/items/{bookId}")
    public ResponseEntity<CartResponse> removeItem(@AuthenticationPrincipal Jwt jwt, @PathVariable String bookId) {
        Cart cart = cartTransactionService.removeBook(currentUserId(jwt), bookId);
        return ResponseEntity.ok(CartResponse.from(cart));
    }

    @DeleteMapping
    public ResponseEntity<CartResponse> clearCart(@AuthenticationPrincipal Jwt jwt) {
        Cart cart = cartTransactionService.clear(currentUserId(jwt));
        return ResponseEntity.ok(CartResponse.from(cart));
    }

    @GetMapping("/count")
    public ResponseEntity<CartCountResponse> getCartCount(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = currentUserId(jwt);
        CartCountResponse response = cartTransactionService.findOpenCart(userId)
                .map(cart -> new CartCountResponse(cart.getTotalUnits(), cart.getDistinctItemCount()))
                .orElseGet(CartCountResponse::empty);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/validate")
    public ResponseEntity<CartValidationResponse> validateCart(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = currentUserId(jwt);
        Cart cart = cartTransactionService.getOpenCart(userId);
        CartValidationResult validationResult = cartTransactionService.validateOpenCart(userId);
        return ResponseEntity.ok(CartValidationResponse.from(cart, validationResult));
    }

    @PostMapping("/checkout")
    public ResponseEntity<CartResponse> checkout(@AuthenticationPrincipal Jwt jwt) {
        Cart cart = cartTransactionService.prepareCheckout(currentUserId(jwt));
        return ResponseEntity.ok(CartResponse.from(cart));
    }

    private UUID currentUserId(Jwt jwt) {
        return authenticatedUserIdResolver.resolve(jwt);
    }
}

