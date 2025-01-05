package com.ruppyrup.productsservice.events.services;

import com.amazonaws.xray.AWSXRay;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruppyrup.productsservice.events.dto.EventType;
import com.ruppyrup.productsservice.events.dto.ProductEventDto;
import com.ruppyrup.productsservice.events.dto.ProductFailureEventDto;
import com.ruppyrup.productsservice.models.Product;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import software.amazon.awssdk.services.sns.model.Topic;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Service
public class EventsPublisher {

    private final SnsAsyncClient snsAsyncClient;
    private final Topic productsEventTopic;
    private final ObjectMapper objectMapper;
    public EventsPublisher(SnsAsyncClient snsAsyncClient, Topic productsEventTopic, ObjectMapper objectMapper) {
      this.snsAsyncClient = snsAsyncClient;
      this.productsEventTopic = productsEventTopic;
      this.objectMapper = objectMapper;
    }

    public CompletableFuture<PublishResponse> sendProductFailureEvent(ProductFailureEventDto productFailureEventDto) throws JsonProcessingException {
        return sendEvent(objectMapper.writeValueAsString(productFailureEventDto), EventType.PRODUCT_FAILURE);
    }

    public CompletableFuture<PublishResponse> sendProductEvent(Product product, EventType eventType, String email) throws JsonProcessingException {
        ProductEventDto productEventDto = new ProductEventDto(product, email);
        return sendEvent(objectMapper.writeValueAsString(productEventDto), eventType);
    }

    private CompletableFuture<PublishResponse> sendEvent(String data, EventType eventType) {
        return snsAsyncClient.publish(PublishRequest.builder()
                .message(data)
                .messageAttributes(Map.of(
                        "eventType", MessageAttributeValue.builder()
                                        .dataType("String")
                                        .stringValue(eventType.toString())
                                .build(),
                        "requestId", MessageAttributeValue.builder()
                                        .dataType("String")
                                        .stringValue(ThreadContext.get("requestId"))
                                .build(),
                        "traceId", MessageAttributeValue.builder()
                                .dataType("String")
                                .stringValue(Objects.requireNonNull(AWSXRay.getCurrentSegment()).getTraceId().toString())
                                .build()
                ))
                .topicArn(this.productsEventTopic.topicArn())
                .build());
    }
}
