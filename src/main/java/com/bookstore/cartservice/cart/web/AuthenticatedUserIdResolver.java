package com.bookstore.cartservice.cart.web;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

@Component
public class AuthenticatedUserIdResolver {

    private static final List<String> CLAIM_NAMES = List.of("userId", "sub");

    public UUID resolve(Jwt jwt) {
        if (jwt == null) {
            throw new InvalidAuthenticationClaimException("Authentication is required");
        }

        for (String claimName : CLAIM_NAMES) {
            String value = jwt.getClaimAsString(claimName);
            if (!StringUtils.hasText(value)) {
                continue;
            }
            try {
                return UUID.fromString(value);
            } catch (IllegalArgumentException exception) {
                throw new InvalidAuthenticationClaimException(
                        "JWT claim '%s' must contain a UUID".formatted(claimName));
            }
        }

        throw new InvalidAuthenticationClaimException("JWT must contain one of the claims: sub, userId");
    }
}

