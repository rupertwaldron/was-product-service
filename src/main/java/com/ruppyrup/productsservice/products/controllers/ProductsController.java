package com.ruppyrup.productsservice.products.controllers;

import com.amazonaws.xray.spring.aop.XRayEnabled;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.ruppyrup.productsservice.dto.ProductDto;
import com.ruppyrup.productsservice.errors.ProductErrors;
import com.ruppyrup.productsservice.events.dto.EventType;
import com.ruppyrup.productsservice.events.services.EventsPublisher;
import com.ruppyrup.productsservice.exceptions.ProductException;
import com.ruppyrup.productsservice.models.Product;
import com.ruppyrup.productsservice.repositories.ProductsRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

@Slf4j
@RestController()
@RequestMapping("/api/products")
@XRayEnabled
public class ProductsController {
    private final ProductsRepository productsRepository;
    private final EventsPublisher eventsPublisher;

    @Value("${asw.sns.notification.email}")
    private String emailNotification;

    @Value("${api.stage}")
    private String stage;

    public ProductsController(ProductsRepository productsRepository, EventsPublisher eventsPublisher) {
        this.productsRepository = productsRepository;
        this.eventsPublisher = eventsPublisher;
    }



    @GetMapping
    public ResponseEntity<?> getAllProducts(@RequestParam(required = false) String code) throws ProductException {
        if (code != null) {
            log.info("Get product by code :: {} in {}", code, stage);
            return Optional.ofNullable(productsRepository.getByCode(code).join())
                    .map(product -> {
                        log.info("Get all products consumed ==> {}", product.consumedCapacity());
                        return new ResponseEntity<>(new ProductDto(product.attributes()), HttpStatus.OK);
                    })
                    .orElseThrow(() -> new ProductException(ProductErrors.PRODUCT_NOT_FOUND, stage, null));
        }

        log.info("Get all products");
        List<ProductDto> productDtos = new ArrayList<>();

        productsRepository.getAll()
                        .subscribe(page -> {
                            log.info("Consumed capacity for scan ==> {}", page.consumedCapacity());
                            page.items().stream()
                                    .map(ProductDto::new)
                                    .forEach(productDtos::add);
                        }).join();

//        productsRepository.getAll()
//                .items()
//                .subscribe(item -> {
//                    productDtos.add(new ProductDto(item));
//                })
//                .join();

        return new ResponseEntity<>(productDtos, HttpStatus.OK);
    }

    @GetMapping("{id}")
    public ResponseEntity<ProductDto> getProductById(@PathVariable("id") String id) throws ProductException {
        log.info("Get product by id :: {} in {}", id, stage);

        return Optional.ofNullable(productsRepository.getById(id).join())
                .map(prod -> {
                    log.info("Consumbed capacity get product by id :: {}", prod.consumedCapacity());
                    return new ResponseEntity<>(new ProductDto(prod.attributes()), HttpStatus.OK);
                })
                .orElseThrow(() -> new ProductException(ProductErrors.PRODUCT_NOT_FOUND, stage, id));
    }

    @PostMapping
    public ResponseEntity<ProductDto> createProduct(@RequestBody ProductDto productDto)
            throws ProductException, JsonProcessingException, ExecutionException, InterruptedException {
        Product createdProduct = productDto.toProduct();
        createdProduct.setId(UUID.randomUUID().toString());
        log.info("Product created with id :: {} in {}", createdProduct.getId(), stage);
        CompletableFuture<Void> productCreate = productsRepository.create(createdProduct);

        CompletableFuture<PublishResponse> publishResponse = eventsPublisher.sendProductEvent(createdProduct, EventType.PRODUCT_CREATED, emailNotification);

        CompletableFuture.allOf(productCreate, publishResponse).join();

        ThreadContext.put("messageId", publishResponse.get().messageId());

        return new ResponseEntity<>(new ProductDto(createdProduct), HttpStatus.CREATED);
    }

    @DeleteMapping("{id}")
    public ResponseEntity<ProductDto> deleteProductById(@PathVariable("id") String id) throws ProductException, JsonProcessingException {
        Product productDeleted = productsRepository.deleteById(id).join();
        if (productDeleted != null) {
            PublishResponse publishResponse = eventsPublisher.sendProductEvent(productDeleted, EventType.PRODUCT_DELETED, emailNotification).join();
            ThreadContext.put("messageId", publishResponse.messageId());

            log.info("Product deleted - ID: {} in {}", productDeleted.getId(), stage);
            return new ResponseEntity<>(new ProductDto(productDeleted), HttpStatus.OK);
        } else {
            throw new ProductException(ProductErrors.PRODUCT_NOT_FOUND, stage, id);
        }
    }

    @PutMapping("{id}")
    public ResponseEntity<ProductDto> updateProductById(@RequestBody ProductDto productDto, @PathVariable("id") String id) throws ProductException, JsonProcessingException {
        try {
            Product updatedProduct = productsRepository.update(id, productDto.toProduct()).join();
            log.info("Update product by id :: {} in {}", updatedProduct.getId(), stage);

            PublishResponse publishResponse = eventsPublisher.sendProductEvent(updatedProduct,EventType.PRODUCT_UPDATED, emailNotification).join();
            ThreadContext.put("messageId", publishResponse.messageId());

            return new ResponseEntity<>(new ProductDto(updatedProduct), HttpStatus.OK);
        } catch (CompletionException e) {
            throw new ProductException(ProductErrors.PRODUCT_NOT_FOUND, stage, id);
        }
    }
}
