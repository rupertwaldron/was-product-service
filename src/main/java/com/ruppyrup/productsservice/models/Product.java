package com.ruppyrup.productsservice.models;

import lombok.Data;
import lombok.Getter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;


@Data
@DynamoDbBean
public class Product {
    private String id;
    private String productName;
    private String code;
    private float price;
    private String model;

    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }
}
