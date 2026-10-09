package com.bookstore.cartservice.cart.integration.catalog;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;

final class HttpCatalogClient implements CatalogClient {

    private final RestClient restClient;

    HttpCatalogClient(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public CatalogBook getBook(String bookId) {
        try {
            CatalogBookResponse response = restClient.get()
                    .uri("/api/catalog/books/{bookId}", bookId)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(CatalogBookResponse.class);
            return map(bookId, response);
        } catch (HttpClientErrorException.NotFound exception) {
            throw new CatalogBookNotFoundException(bookId, exception);
        } catch (ResourceAccessException exception) {
            throw new CatalogServiceUnavailableException("Catalog service is unavailable", exception);
        } catch (RestClientResponseException exception) {
            throw new CatalogResponseException("Catalog service returned an unexpected response: "
                    + exception.getStatusCode(), exception);
        } catch (RestClientException exception) {
            throw new CatalogServiceUnavailableException("Catalog service call failed", exception);
        }
    }

    private static CatalogBook map(String requestedBookId, CatalogBookResponse response) {
        if (response == null) {
            throw new CatalogResponseException("Catalog service returned an empty response body");
        }
        if (response.pricing() == null) {
            throw new CatalogResponseException("Catalog response missing pricing details for book " + requestedBookId);
        }
        if (response.stock() == null || response.stock() < 0) {
            throw new CatalogResponseException("Catalog response contains invalid stock for book " + requestedBookId);
        }

        String responseBookId = parseBookId(requestedBookId, response.id());
        String currency = response.pricing().currency();
        if (currency == null || currency.isBlank()) {
            throw new CatalogResponseException("Catalog response missing currency for book " + requestedBookId);
        }

        Integer priceMinorUnits = response.pricing().salePrice() != null
                ? response.pricing().salePrice()
                : response.pricing().listPrice();
        if (priceMinorUnits == null || priceMinorUnits < 0) {
            throw new CatalogResponseException("Catalog response contains invalid pricing for book " + requestedBookId);
        }

        return new CatalogBook(
                responseBookId,
                BigDecimal.valueOf(priceMinorUnits.longValue(), 2),
                currency,
                response.stock()
        );
    }

    private static String parseBookId(String requestedBookId, String rawBookId) {
        if (rawBookId == null || rawBookId.isBlank()) {
            throw new CatalogResponseException("Catalog response missing book id for book " + requestedBookId);
        }
        return rawBookId;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CatalogBookResponse(String id, Pricing pricing, Long stock) {

        @JsonIgnoreProperties(ignoreUnknown = true)
        private record Pricing(String currency, Integer salePrice, Integer listPrice) {
        }
    }
}

