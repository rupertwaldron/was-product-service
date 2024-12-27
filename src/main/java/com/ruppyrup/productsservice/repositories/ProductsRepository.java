package com.ruppyrup.productsservice.repositories;

import com.amazonaws.xray.spring.aop.XRayEnabled;
import com.ruppyrup.productsservice.models.Product;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.PagePublisher;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;

import java.util.concurrent.CompletableFuture;

@Repository
@XRayEnabled
public class ProductsRepository {
    private final DynamoDbAsyncTable<Product> productsTable;

    public ProductsRepository(DynamoDbEnhancedAsyncClient dynamoDbClient,
                              @Value("${aws.productsddb.name}") String productsDdbName) {
        this.productsTable = dynamoDbClient.table(productsDdbName, TableSchema.fromBean(Product.class));
    }

    public PagePublisher<Product> getAll() {
        // Do not do this in production
        return productsTable.scan();
    }

    public CompletableFuture<Product> getById(String productId) {
        return productsTable.getItem(Key.builder()
                .partitionValue(productId)
                .build());
    }

    public CompletableFuture<Void> create(Product product) {
        return productsTable.putItem(product);
    }

    public CompletableFuture<Product> deleteById(String productId) {
        return productsTable.deleteItem(Key.builder()
                .partitionValue(productId)
                .build());
    }

    public CompletableFuture<Product> update(String productId, Product product) {
        product.setId(productId);
        return productsTable.updateItem(
                UpdateItemEnhancedRequest.
                                builder(Product.class)
                        .item(product)
                        .conditionExpression(Expression.builder()
                                .expression("attribute_exists(id)")
                                .build())
                        .build()
        );
    }
}
