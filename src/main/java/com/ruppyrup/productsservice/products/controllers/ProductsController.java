package com.ruppyrup.productsservice.products.controllers;

import com.ruppyrup.productsservice.dto.ProductDto;
import com.ruppyrup.productsservice.models.Product;
import com.ruppyrup.productsservice.repositories.ProductsRepository;
import org.apache.logging.log4j.LogManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionException;


@RestController()
@RequestMapping("/api/products")
public class ProductsController {

    private static final Logger LOG = LogManager.getLogger(ProductsController.class);
    private final ProductsRepository productsRepository;

    public ProductsController(ProductsRepository productsRepository) {
        this.productsRepository = productsRepository;
    }

    @GetMapping
    public ResponseEntity<List<ProductDto>> getAllProducts() {
        LOG.info("Get all products");
        List<ProductDto> productDtos = new ArrayList<>();

        productsRepository.getAll()
                .items()
                .subscribe(item -> productDtos.add(new ProductDto(item)))
                .join();

        return new ResponseEntity<>(productDtos, HttpStatus.OK);
    }

    @GetMapping("{id}")
    public ResponseEntity<ProductDto> getProductById(@PathVariable("id") String id) {
        LOG.info("Get product by id :: " + id);

        return Optional.ofNullable(productsRepository.getById(id).join())
                .map(prod -> new ResponseEntity<>(new ProductDto(prod), HttpStatus.OK))
                .orElse(new ResponseEntity<>(new ProductDto(), HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public ResponseEntity<ProductDto> createProduct(@RequestBody ProductDto productDto) {
        Product createdProduct = productDto.toProduct();
        createdProduct.setId(UUID.randomUUID().toString());
        LOG.info("Product created with id :: " + createdProduct.getId());

        productsRepository.create(createdProduct).join();
        return new ResponseEntity<>(new ProductDto(createdProduct), HttpStatus.CREATED);
    }

    @DeleteMapping("{id}")
    public ResponseEntity<ProductDto> deleteProductById(@PathVariable("id") String id) {
        LOG.info("Delete product by id :: " + id);

        return Optional.ofNullable(productsRepository.deleteById(id).join())
                .map(prod -> new ResponseEntity<>(new ProductDto(prod), HttpStatus.OK))
                .orElse(new ResponseEntity<>(new ProductDto(), HttpStatus.NOT_FOUND));
    }

    @PutMapping("{id}")
    public ResponseEntity<ProductDto> updateProductById(@RequestBody ProductDto productDto, @PathVariable("id") String id) {
        try {
            Product updatedProduct = productsRepository.update(id, productDto.toProduct()).join();
            LOG.info("Update product by id :: " + updatedProduct.getId());

            return new ResponseEntity<>(new ProductDto(updatedProduct), HttpStatus.OK);
        } catch (CompletionException e) {
            return new ResponseEntity<>(new ProductDto(), HttpStatus.NOT_FOUND);
        }
    }
}
