package com.ruppyrup.productsservice.exceptions;

import com.ruppyrup.productsservice.errors.ProductErrors;
import lombok.Getter;
import org.springframework.lang.Nullable;


@Getter
public class ProductException extends Exception {
    private final ProductErrors errors;
    private final String stage;
    @Nullable
    private final String productId;

    public ProductException(ProductErrors errors, String stage, @Nullable String productId) {
        this.errors = errors;
        this.stage = stage;
        this.productId = productId;
    }
}
