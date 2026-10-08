package com.bookstore.cartservice.cart.web;

import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.*;

@Configuration
public class CartSecurityConfig {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(CartSecurityConfig.class);
    @Bean
    SecurityFilterChain cartSecurityFilterChain(HttpSecurity http) {
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers("/api/carts/**").authenticated()
                        .anyRequest().denyAll()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                writeSecurityError(response, HttpStatus.UNAUTHORIZED, "Authentication is required", request.getRequestURI()))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeSecurityError(response, HttpStatus.FORBIDDEN, "Access denied", request.getRequestURI()))
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwtConfigurer -> jwtConfigurer.jwtAuthenticationConverter(jwtAuthenticationConverter())));

        return http.build();
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(this::extractAuthorities);
        logger.info("JWT Authentication Converter configured with custom authority extraction");
        return converter;
    }

    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.addAll(extractClaim(jwt, "role", "ROLE_"));
        authorities.addAll(extractClaim(jwt, "roles", "ROLE_"));
        authorities.addAll(extractClaim(jwt, "authorities", "ROLE_"));
        authorities.addAll(extractClaim(jwt, "scope", "SCOPE_"));
        authorities.addAll(extractClaim(jwt, "scp", "SCOPE_"));
        return authorities;
    }

    private Collection<GrantedAuthority> extractClaim(Jwt jwt, String claimName, String prefix) {
        Object value = jwt.getClaims().get(claimName);
        if (value == null) {
            return List.of();
        }

        List<String> values;
        if (value instanceof String stringValue) {
            values = Arrays.stream(stringValue.split("\\s+")).toList();
        } else if (value instanceof Collection<?> collection) {
            values = collection.stream().map(String::valueOf).toList();
        } else {
            return List.of();
        }

        return values.stream()
                .filter(v -> !v.isBlank())
                .map(v -> prefix.equals("ROLE_") ? v.toUpperCase() : v)
                .map(v -> v.startsWith(prefix) ? v : prefix + v)
                .map(SimpleGrantedAuthority::new)
                .map(GrantedAuthority.class::cast)
                .toList();
    }

    @Bean
    JwtDecoder jwtDecoder(Environment environment, ResourceLoader resourceLoader) {
        String publicKeyLocation = environment.getProperty("cart.security.jwt.public-key-location");
        if (!StringUtils.hasText(publicKeyLocation)) {
            publicKeyLocation = environment.getProperty("spring.security.oauth2.resourceserver.jwt.public-key-location");
        }

        String jwkSetUri = environment.getProperty("cart.security.jwt.jwk-set-uri");
        if (!StringUtils.hasText(jwkSetUri)) {
            jwkSetUri = environment.getProperty("spring.security.oauth2.resourceserver.jwt.jwk-set-uri");
        }

        if (StringUtils.hasText(publicKeyLocation)) {
            return NimbusJwtDecoder.withPublicKey(loadPublicKey(resourceLoader, publicKeyLocation)).build();
        }

        if (StringUtils.hasText(jwkSetUri)) {
            return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        }

        return token -> {
            throw new JwtException("JWT decoder not configured. Set cart.security.jwt.public-key-location or cart.security.jwt.jwk-set-uri");
        };
    }

    private RSAPublicKey loadPublicKey(ResourceLoader resourceLoader, String publicKeyLocation) {
        Resource resource = resourceLoader.getResource(publicKeyLocation);
        try (InputStream inputStream = resource.getInputStream()) {
            return RsaKeyConverters.x509().convert(inputStream);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to load RSA public key from " + publicKeyLocation, ex);
        }
    }

    private void writeSecurityError(HttpServletResponse response, HttpStatus status, String message, String path) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), Map.of(
                "timestamp", Instant.now().toString(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message,
                "path", path,
                "details", Map.of()
        ));
    }
}

