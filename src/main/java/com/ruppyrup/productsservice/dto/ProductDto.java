package com.ruppyrup.productsservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ruppyrup.productsservice.models.Product;

public record ProductDto(
        String id, String name, String code, float price, String model,
        @JsonInclude(JsonInclude.Include.NON_NULL) String url,
        @JsonInclude(JsonInclude.Include.NON_NULL) Long expiresAt
) {

    public ProductDto(Product product) {
        this(product.getId(), product.getProductName(), product.getCode(), product.getPrice(), product.getModel(), product.getProductUrl(), product.getExpiresAt());
    }

    public ProductDto() {
        this("Missing", "Missing", "Missing", 0, "Missing", "No url", 0L);
    }

    public Product toProduct() {
        var product = new Product();
        product.setId(this.id());
        product.setProductName(this.name());
        product.setCode(this.code());
        product.setPrice(this.price());
        product.setModel(this.model());
        product.setProductUrl(this.url());
        product.setExpiresAt(this.expiresAt());
        return product;
    }
}
