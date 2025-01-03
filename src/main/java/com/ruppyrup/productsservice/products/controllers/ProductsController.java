package com.ruppyrup.productsservice.products.controllers;

import com.amazonaws.xray.spring.aop.XRayEnabled;
import com.ruppyrup.productsservice.dto.ProductDto;
import com.ruppyrup.productsservice.errors.ProductErrors;
import com.ruppyrup.productsservice.exceptions.ProductException;
import com.ruppyrup.productsservice.models.Product;
import com.ruppyrup.productsservice.repositories.ProductsRepository;
import lombok.extern.slf4j.Slf4j;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionException;

@Slf4j
@RestController()
@RequestMapping("/api/products")
@XRayEnabled
public class ProductsController {
    private final ProductsRepository productsRepository;

    public ProductsController(ProductsRepository productsRepository) {
        this.productsRepository = productsRepository;
    }

    @GetMapping
    public ResponseEntity<?> getAllProducts(@RequestParam(required = false) String code) throws ProductException {
        if (code != null) {
            log.info("Get product by code :: {}", code);
            return Optional.ofNullable(productsRepository.getByCode(code).join())
                    .map(product -> new ResponseEntity<>(new ProductDto(product), HttpStatus.OK))
                    .orElseThrow(() -> new ProductException(ProductErrors.PRODUCT_NOT_FOUND, null));
        }

        log.info("Get all products");
        List<ProductDto> productDtos = new ArrayList<>();

        productsRepository.getAll()
                .items()
                .subscribe(item -> productDtos.add(new ProductDto(item)))
                .join();

        return new ResponseEntity<>(productDtos, HttpStatus.OK);
    }

    @GetMapping("{id}")
    public ResponseEntity<ProductDto> getProductById(@PathVariable("id") String id) throws ProductException {
        log.info("Get product by id :: {}", id);

        return Optional.ofNullable(productsRepository.getById(id).join())
                .map(prod -> new ResponseEntity<>(new ProductDto(prod), HttpStatus.OK))
                .orElseThrow(() -> new ProductException(ProductErrors.PRODUCT_NOT_FOUND, id));
    }

    @PostMapping
    public ResponseEntity<ProductDto> createProduct(@RequestBody ProductDto productDto) throws ProductException {
        Product createdProduct = productDto.toProduct();
        createdProduct.setId(UUID.randomUUID().toString());
        log.info("Product created with id :: {}", createdProduct.getId());

        productsRepository.create(createdProduct).join();
        return new ResponseEntity<>(new ProductDto(createdProduct), HttpStatus.CREATED);
    }

    @DeleteMapping("{id}")
    public ResponseEntity<ProductDto> deleteProductById(@PathVariable("id") String id) throws ProductException {
        log.info("Delete product by id :: {}", id);

        return Optional.ofNullable(productsRepository.deleteById(id).join())
                .map(prod -> new ResponseEntity<>(new ProductDto(prod), HttpStatus.OK))
                .orElseThrow(() -> new ProductException(ProductErrors.PRODUCT_NOT_FOUND, id));
    }

    @PutMapping("{id}")
    public ResponseEntity<ProductDto> updateProductById(@RequestBody ProductDto productDto, @PathVariable("id") String id) throws ProductException {
        try {
            Product updatedProduct = productsRepository.update(id, productDto.toProduct()).join();
            log.info("Update product by id :: {}", updatedProduct.getId());

            return new ResponseEntity<>(new ProductDto(updatedProduct), HttpStatus.OK);
        } catch (CompletionException e) {
            throw new ProductException(ProductErrors.PRODUCT_NOT_FOUND, id);
        }
    }
}
