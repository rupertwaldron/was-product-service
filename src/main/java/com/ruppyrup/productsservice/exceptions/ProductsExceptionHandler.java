package com.ruppyrup.productsservice.exceptions;

import com.ruppyrup.productsservice.errors.ProductErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice
public class ProductsExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ProductException.class)
    protected ResponseEntity<Object> handleProductException(ProductException ex, WebRequest request) {
        ProductErrorResponse productErrorResponse = new ProductErrorResponse(
                ex.getErrors().getMessage(),
                ex.getErrors().getStatus().value(),
                ThreadContext.get("requestId"),
                ex.getProductId()
        );

        log.error(ex.getErrors().getMessage());

        return handleExceptionInternal(
                ex,
                productErrorResponse,
                new HttpHeaders(),
                ex.getErrors().getStatus(),
                request
        );
    }
}
