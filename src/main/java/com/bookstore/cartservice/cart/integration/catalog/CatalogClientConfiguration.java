package com.bookstore.cartservice.cart.integration.catalog;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({CatalogServiceProperties.class, CartRulesProperties.class})
public class CatalogClientConfiguration {

    @Bean
    RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    CatalogClient catalogClient(RestClient.Builder restClientBuilder, CatalogServiceProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getConnectTimeout());
        requestFactory.setReadTimeout(properties.getReadTimeout());

        RestClient restClient = restClientBuilder
                .baseUrl(properties.getBaseUrl().toString())
                .requestFactory(requestFactory)
                .build();

        return new HttpCatalogClient(restClient);
    }


}
