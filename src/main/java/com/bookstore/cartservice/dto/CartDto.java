package com.bookstore.cartservice.dto;

import lombok.Data;

import java.util.List;

@Data
public class CartDto {
    private String id;
    private String userId;
    private Double totalPrice;
    private Integer totalQuantity;
    private List<CartItemDto> items;
}
