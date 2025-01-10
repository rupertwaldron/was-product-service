package com.ruppyrup.productsservice.repositories;

import com.amazonaws.xray.spring.aop.XRayEnabled;
import com.ruppyrup.productsservice.errors.ProductErrors;
import com.ruppyrup.productsservice.exceptions.ProductException;
import com.ruppyrup.productsservice.models.Product;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.PagePublisher;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Repository
@XRayEnabled
public class ProductsRepository {
    private final DynamoDbAsyncTable<Product> productsTable;

    @Value("${api.stage}")
    private String stage;

    public ProductsRepository(DynamoDbEnhancedAsyncClient dynamoDbClient,
                              @Value("${aws.productsddb.name}") String productsDdbName) {
        this.productsTable = dynamoDbClient.table(productsDdbName, TableSchema.fromBean(Product.class));
    }

    private CompletableFuture<Product> checkIfCodeExists(String code) {
        List<Product> products = new ArrayList<>();
        productsTable.index("codeIdx").query(QueryEnhancedRequest.builder()
                        .limit(1)
                        .queryConditional(QueryConditional.keyEqualTo(Key.builder()
                                        .partitionValue(code)
                                .build()))
                .build()).subscribe(productPage -> {
                    products.addAll(productPage.items());

        }).join();

        if (!products.isEmpty()) {
            return CompletableFuture.supplyAsync(products::getFirst);
        } else {
            return CompletableFuture.supplyAsync(() -> null);
        }
    }

    public CompletableFuture<Product> getByCode(String code) {
        Product productByCode = checkIfCodeExists(code).join();
        if (productByCode != null) {
            return getById(productByCode.getId());
        } else {
            return CompletableFuture.supplyAsync(() -> null);
        }
    }

    public PagePublisher<Product> getAll() {
        // Do not do this in production
        return productsTable.scan();
    }

    public CompletableFuture<Product> getById(String productId) {
        log.info("ProductId :: {}", productId);
        return productsTable.getItem(Key.builder()
                .partitionValue(productId)
                .build());
    }

    public CompletableFuture<Void> create(Product product) throws ProductException {
        Product productWithSameCode = checkIfCodeExists(product.getCode()).join();
        if (productWithSameCode != null) {
            throw new ProductException(ProductErrors.PRODUCT_CODE_ALREADY_EXISTS, stage, productWithSameCode.getId());
        }
        return productsTable.putItem(product);
    }

    public CompletableFuture<Product> deleteById(String productId) {
        return productsTable.deleteItem(Key.builder()
                .partitionValue(productId)
                .build());
    }

    public CompletableFuture<Product> update(String productId, Product product) throws ProductException {
        product.setId(productId);
        Product productWithSameCode = checkIfCodeExists(product.getCode()).join();
        if (productWithSameCode != null && !productWithSameCode.getId().equals(product.getId())) {
            throw new ProductException(ProductErrors.PRODUCT_CODE_ALREADY_EXISTS, stage, productWithSameCode.getId());
        }
        return productsTable.updateItem(
                UpdateItemEnhancedRequest.builder(Product.class)
                        .item(product)
                        .conditionExpression(Expression.builder()
                                .expression("attribute_exists(id)")
                                .build())
                        .build()
        );
    }
}
