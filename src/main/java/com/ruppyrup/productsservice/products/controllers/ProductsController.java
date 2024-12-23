package com.ruppyrup.productsservice.products.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.LogManager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.apache.logging.log4j.Logger;




@RestController()
@RequestMapping("/api/products")
public class ProductsController {

    private static final Logger LOG = LogManager.getLogger(ProductsController.class);

    @GetMapping
    public String getAllProducts() {
        LOG.info("Get all products");
        return "All products";
    }

}
