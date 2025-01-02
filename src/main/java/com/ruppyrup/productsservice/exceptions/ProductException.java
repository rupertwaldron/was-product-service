package com.ruppyrup.productsservice.exceptions;

import com.ruppyrup.productsservice.errors.ProductErrors;
import lombok.Getter;
import org.springframework.lang.Nullable;


@Getter
public class ProductException extends Exception {
    private final ProductErrors errors;
    @Nullable
    private final String productId;

    public ProductException(ProductErrors errors, @Nullable String productId) {
        this.errors = errors;
        this.productId = productId;
    }
}
