package com.bookstore.cartservice.cart.web;

import com.bookstore.cartservice.cart.application.CartCheckoutValidationException;
import com.bookstore.cartservice.cart.application.CartTransactionService;
import com.bookstore.cartservice.cart.application.CartValidationIssue;
import com.bookstore.cartservice.cart.application.CartValidationResult;
import com.bookstore.cartservice.cart.domain.Cart;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CartController.class)
@Import({CartSecurityConfig.class, CartExceptionHandler.class, AuthenticatedUserIdResolver.class})
class CartControllerTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID BOOK_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CartTransactionService cartTransactionService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void getCartRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/carts"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required"));
    }

    @Test
    void addItemReturnsCreatedCartResponse() throws Exception {
        Cart cart = activeCartWithSingleItem();
        when(cartTransactionService.addBook(USER_ID, BOOK_ID, 2)).thenReturn(cart);

        mockMvc.perform(post("/api/carts/items")
                        .with(jwt().jwt(token -> token.claim("sub", USER_ID.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "bookId": "%s",
                                  "quantity": 2
                                }
                                """.formatted(BOOK_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.items[0].bookId").value(BOOK_ID.toString()))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.totalUnits").value(2))
                .andExpect(jsonPath("$.distinctItemCount").value(1))
                .andExpect(jsonPath("$.subtotal").value(25.00));
    }

    @Test
    void updateItemRejectsInvalidRequestBody() throws Exception {
        mockMvc.perform(put("/api/carts/items/{bookId}", BOOK_ID)
                        .with(jwt().jwt(token -> token.claim("sub", USER_ID.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" + "\"quantity\":0" + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details.quantity").value("quantity must be greater than zero"));
    }

    @Test
    void cartCountReturnsZeroWhenCartDoesNotExist() throws Exception {
        when(cartTransactionService.findOpenCart(USER_ID)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/carts/count")
                        .with(jwt().jwt(token -> token.claim("sub", USER_ID.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUnits").value(0))
                .andExpect(jsonPath("$.distinctItemCount").value(0));
    }

    @Test
    void validateReturnsStructuredIssuesWithoutFailingRequest() throws Exception {
        Cart cart = activeCartWithSingleItem();
        CartValidationResult validationResult = CartValidationResult.failure(List.of(
                new CartValidationIssue(BOOK_ID, "PRICE_CHANGED", "Catalog price has changed", 2, 5L,
                        new BigDecimal("12.50"), new BigDecimal("13.00"))
        ));
        when(cartTransactionService.getOpenCart(USER_ID)).thenReturn(cart);
        when(cartTransactionService.validateOpenCart(USER_ID)).thenReturn(validationResult);

        mockMvc.perform(post("/api/carts/validate")
                        .with(jwt().jwt(token -> token.claim("sub", USER_ID.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.issues[0].code").value("PRICE_CHANGED"))
                .andExpect(jsonPath("$.issues[0].currentUnitPrice").value(13.00));
    }

    @Test
    void checkoutReturnsValidationFailureAsUnprocessableEntity() throws Exception {
        CartValidationResult validationResult = CartValidationResult.failure(List.of(
                new CartValidationIssue(BOOK_ID, "STOCK_UNAVAILABLE", "Requested quantity is no longer available", 2, 1L,
                        new BigDecimal("12.50"), new BigDecimal("12.50"))
        ));
        when(cartTransactionService.prepareCheckout(USER_ID))
                .thenThrow(new CartCheckoutValidationException(validationResult));

        mockMvc.perform(post("/api/carts/checkout")
                        .with(jwt().jwt(token -> token.claim("sub", USER_ID.toString()))))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.message").value("Cart validation failed"));
    }

    @Test
    void nonUuidJwtSubjectReturnsUnauthorized() throws Exception {
        mockMvc.perform(delete("/api/carts")
                        .with(jwt()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("JWT claim 'sub' must contain a UUID"));
    }

    private static Cart activeCartWithSingleItem() {
        Cart cart = Cart.createFor(USER_ID);
        cart.addBook(BOOK_ID, 2, new BigDecimal("12.50"));
        return cart;
    }
}

