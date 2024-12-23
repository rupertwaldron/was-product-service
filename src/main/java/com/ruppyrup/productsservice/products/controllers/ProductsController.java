package com.ruppyrup.productsservice.products.controllers;

import com.ruppyrup.productsservice.dto.ProductDto;
import com.ruppyrup.productsservice.models.Product;
import com.ruppyrup.productsservice.repositories.ProductsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.LogManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.enhanced.dynamodb.model.PagePublisher;

import javax.net.ssl.SSLEngineResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


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
        return new ResponseEntity<>(productDto, HttpStatus.CREATED);
    }

}
