package com.ruppyrup.productsservice.events.services;

import com.ruppyrup.productsservice.events.dto.EventType;
import com.ruppyrup.productsservice.events.dto.ProductFailureEventDto;
import com.ruppyrup.productsservice.models.Product;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@Profile("local")
public class EventsPublisherLocal extends EventsPublisher {
    public EventsPublisherLocal() {
        super(null, null, null);
    }

    @Override
    public CompletableFuture<PublishResponse> sendProductFailureEvent(ProductFailureEventDto productFailureEventDto) {
        log.info("*******sendProductFailureEvent*********");
        return CompletableFuture.completedFuture(PublishResponse.builder()
                .messageId("Local_Messsage_Id")
                .build());
    }

    @Override
    public CompletableFuture<PublishResponse> sendProductEvent(Product product, EventType eventType, String email) {
        log.info("*******sendProductSuccessEvent*********");
        return CompletableFuture.completedFuture(PublishResponse.builder()
                .messageId(product.getId())
                .build());
    }
}
