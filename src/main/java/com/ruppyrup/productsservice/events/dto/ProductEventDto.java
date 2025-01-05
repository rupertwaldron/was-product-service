package com.ruppyrup.productsservice.events.dto;

import com.ruppyrup.productsservice.models.Product;

public record ProductEventDto(
        String id,
        String code,
        String email,
        float price
) {

    public ProductEventDto(Product product, String email) {
        this(product.getId(), product.getCode(), email, product.getPrice());
    }
}
