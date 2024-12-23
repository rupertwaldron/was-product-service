package com.ruppyrup.productsservice.dto;

import com.ruppyrup.productsservice.models.Product;

public record ProductDto(
        String id, String name, String code, float price, String model
) {

    public ProductDto(Product product) {
        this(product.getId(), product.getProductName(), product.getCode(), product.getPrice(), product.getModel());
    }

    public ProductDto() {
        this("Missing", "Missing", "Missing", 0, "Missing");
    }

    public Product toProduct() {
        var product = new Product();
        product.setId(this.id());
        product.setProductName(this.name());
        product.setCode(this.code());
        product.setPrice(this.price());
        product.setModel(this.model());
        return product;
    }
}
