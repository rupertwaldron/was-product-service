package com.ruppyrup.productsservice.errors;

import lombok.Getter;
import org.springframework.http.HttpStatus;


@Getter
public enum ProductErrors {
    PRODUCT_NOT_FOUND("Product not found", HttpStatus.NOT_FOUND),
    PRODUCT_CODE_ALREADY_EXISTS("Product already exists", HttpStatus.CONFLICT),
    ;

    private final String message;
    private final HttpStatus status;

    ProductErrors(String message, HttpStatus httpStatus) {
        this.message = message;
        this.status = httpStatus;
    }
}
