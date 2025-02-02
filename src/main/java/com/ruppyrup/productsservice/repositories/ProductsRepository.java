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
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedResponse;
import software.amazon.awssdk.enhanced.dynamodb.model.PagePublisher;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ReturnConsumedCapacity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.numberValue;

@Slf4j
@Repository
@XRayEnabled
public class ProductsRepository {
    private final DynamoDbAsyncTable<Product> productsTable;

    @Value("${api.stage}")
    private String stage;

    public ProductsRepository(DynamoDbEnhancedAsyncClient dynamoDbClient,
                              @Value("${aws.productsddb.name}") String productsDdbName) {
//        EasyRandomParameters parameters = new EasyRandomParameters();
//        parameters.seed(123L);
//        parameters.randomize(Float.class, new FloatRangeRandomizer(10.0f, 1000.0f));
//        parameters.randomize(String.class, new StringRandomizer(StandardCharsets.UTF_8, 5, 20,123));
//
//        EasyRandom easyRandom = new EasyRandom(parameters);
//
        this.productsTable = dynamoDbClient.table(productsDdbName, TableSchema.fromBean(Product.class));

//
//        for (int i = 0; i < 10000; i++) {
//            productsTable.putItem(easyRandom.nextObject(Product.class));
//        }
    }

    private CompletableFuture<Product> checkIfCodeExists(String code) {
        List<Product> products = new ArrayList<>();
        productsTable.index("codeIdx").query(QueryEnhancedRequest.builder()
                .limit(1)
                .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
                .queryConditional(QueryConditional.keyEqualTo(Key.builder()
                        .partitionValue(code)
                        .build()))
                .build()).subscribe(productPage -> {
            log.info("Check if code exist consumed ==> {}", productPage.consumedCapacity());
            products.addAll(productPage.items());

        }).join();

        if (!products.isEmpty()) {
            return CompletableFuture.supplyAsync(products::getFirst);
        } else {
            return CompletableFuture.supplyAsync(() -> null);
        }
    }

    public CompletableFuture<GetItemEnhancedResponse<Product>> getByCode(String code) {
        Product productByCode = checkIfCodeExists(code).join();
        if (productByCode != null) {
            return getById(productByCode.getId());
        } else {
            return CompletableFuture.supplyAsync(() -> null);
        }
    }

    public PagePublisher<Product> getAll() {
        // Do not do this in production
        return productsTable.scan(ScanEnhancedRequest.builder()
                .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
                .build());
    }

    public CompletableFuture<GetItemEnhancedResponse<Product>> getById(String productId) {
        log.info("ProductId :: {}", productId);
        return productsTable.getItemWithResponse(GetItemEnhancedRequest.builder()
                .key(Key.builder()
                        .partitionValue(productId)
                        .build())
                .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
                .build()
        );

    }

    public CompletableFuture<Void> create(Product product) throws ProductException {
        Product productWithSameCode = checkIfCodeExists(product.getCode()).join();
        if (productWithSameCode != null) {
            throw new ProductException(ProductErrors.PRODUCT_CODE_ALREADY_EXISTS, stage, productWithSameCode.getId());
        }
        return productsTable.putItem(product);
    }

    public CompletableFuture<Void> createWithTtl(Product product, String ttl) throws ProductException {
        Product productWithSameCode = checkIfCodeExists(product.getCode()).join();
        if (productWithSameCode != null) {
            throw new ProductException(ProductErrors.PRODUCT_CODE_ALREADY_EXISTS, stage, productWithSameCode.getId());
        }

        long ttlInSeconds = Instant.now().getEpochSecond() + Integer.parseInt(ttl);

        product.setExpiresAt(ttlInSeconds);

        return productsTable.putItem(product);
    }

    public CompletableFuture<Product> updateIfPriceLimitFromCurrent(String productId, Product product, String limit) throws ProductException {
        Integer limitValue = Integer.valueOf(limit);

        product.setId(productId);
        Product productWithSameCode = checkIfCodeExists(product.getCode()).join();
        if (productWithSameCode != null && !productWithSameCode.getId().equals(product.getId())) {
            throw new ProductException(ProductErrors.PRODUCT_CODE_ALREADY_EXISTS, stage, productWithSameCode.getId());
        }

        float newPrice = product.getPrice();

        float minValue = newPrice * (1 - limitValue / 100F);
        float maxValue = newPrice * (1 + limitValue / 100F);

        return productsTable.updateItem(
                UpdateItemEnhancedRequest.builder(Product.class)
                        .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
                        .item(product)
                        .conditionExpression(Expression.builder()
                                .expression("price >= :min_value AND price <= :max_value")
                                .expressionValues(Map.of(
                                        ":min_value", numberValue(minValue),
                                        ":max_value", numberValue(maxValue)))
                                .build())
                        .build()
        );
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
                        .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
                        .item(product)
                        .conditionExpression(Expression.builder()
                                .expression("attribute_exists(id)")
                                .build())
                        .build()
        );
    }
}
