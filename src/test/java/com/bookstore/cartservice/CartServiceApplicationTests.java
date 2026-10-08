package com.bookstore.cartservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;

class CartServiceApplicationTests {

    @Test
    void applicationClassIsAnnotatedAsSpringBootApplication() {
        SpringBootApplication annotation = CartServiceApplication.class.getAnnotation(SpringBootApplication.class);
        org.junit.jupiter.api.Assertions.assertNotNull(annotation);
    }

}
