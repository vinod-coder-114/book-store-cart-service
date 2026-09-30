package com.bookstore.cartservice.dto;

import lombok.Data;

@Data
public class CartItemDto {
    private String id;
    private String productId;
    private Integer quantity;
    private Double price;
}
