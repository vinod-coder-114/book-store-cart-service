package com.bookstore.cartservice.cart.integration.catalog;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cart.rules")
public class CartRulesProperties {

    private int maxQuantityPerItem = 20;

    public int getMaxQuantityPerItem() {
        return maxQuantityPerItem;
    }

    public void setMaxQuantityPerItem(int maxQuantityPerItem) {
        this.maxQuantityPerItem = maxQuantityPerItem;
    }
}

