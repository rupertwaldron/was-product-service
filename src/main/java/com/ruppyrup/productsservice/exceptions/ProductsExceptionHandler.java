package com.ruppyrup.productsservice.exceptions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.ruppyrup.productsservice.errors.ProductErrorResponse;
import com.ruppyrup.productsservice.events.dto.ProductFailureEventDto;
import com.ruppyrup.productsservice.events.services.EventsPublisher;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import java.util.concurrent.CompletableFuture;

@Slf4j
@RestControllerAdvice
public class ProductsExceptionHandler extends ResponseEntityExceptionHandler {

    private final EventsPublisher eventsPublisher;

    @Value("${asw.sns.notification.email}")
    private String emailNotification;

    public ProductsExceptionHandler(EventsPublisher eventsPublisher) {
        this.eventsPublisher = eventsPublisher;
    }

    @ExceptionHandler(ProductException.class)
    protected ResponseEntity<Object> handleProductException(ProductException productException, WebRequest request) throws JsonProcessingException {
        ProductErrorResponse productErrorResponse = new ProductErrorResponse(
                productException.getErrors().getMessage(),
                productException.getErrors().getStatus().value(),
                ThreadContext.get("requestId"),
                productException.getProductId()
        );

        ProductFailureEventDto productFailureEventDto = new ProductFailureEventDto(
                emailNotification,
                productException.getErrors().getStatus().value(),
                productException.getErrors().getMessage(),
                productException.getProductId()
        );

        PublishResponse publishResponse= eventsPublisher.sendProductFailureEvent(productFailureEventDto).join();
        ThreadContext.put("messageId", publishResponse.messageId());

        log.error(productException.getErrors().getMessage());

        return handleExceptionInternal(
                productException,
                productErrorResponse,
                new HttpHeaders(),
                productException.getErrors().getStatus(),
                request
        );
    }
}
