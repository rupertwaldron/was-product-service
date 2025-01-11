package com.ruppyrup.productsservice.config;

import com.amazonaws.xray.interceptors.TracingInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClientBuilder;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.CreateTableResponse;
import software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ListTablesResponse;
import software.amazon.awssdk.services.dynamodb.model.Projection;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Configuration
public class DynamoDBConfig {
    @Value("${amazon.dynamodb.endpoint}")
    private String amazonDynamoDBEndpoint;

    @Value("${aws.region}")
    private String awsRegion;

    @Value("${aws.productsddb.name}")
    private String productsDBName;

    @Bean
    public DynamoDbAsyncClient dynamoDbAsyncClient() {
        var clientdBBuilder = DynamoDbAsyncClient.builder()
                .credentialsProvider(DefaultCredentialsProvider.create())
                .region(Region.of(awsRegion))
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .addExecutionInterceptor(new TracingInterceptor())
                        .build());

        if (amazonDynamoDBEndpoint.isBlank()) {
            return clientdBBuilder.build();
        } else {
            var clientdB = buildLocaldBClient(clientdBBuilder);

            createTableIfNoneExists(clientdB, productsDBName);

            return clientdB;
        }
    }

    @Bean
    public DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient() {
        return DynamoDbEnhancedAsyncClient.builder()
                .dynamoDbClient(dynamoDbAsyncClient())
                .build();
    }

    private DynamoDbAsyncClient buildLocaldBClient(DynamoDbAsyncClientBuilder clientdBBuilder) {
        clientdBBuilder.endpointOverride(URI.create(amazonDynamoDBEndpoint));
        return clientdBBuilder.build();
    }

    private static void createTableIfNoneExists(DynamoDbAsyncClient clientdB, String tableName) {
        ListTablesResponse join = clientdB.listTables().join();

        if (join.tableNames().contains("products")) {
            log.info("Setting up table with response {}", tableName);
            return;
        }

        CompletableFuture<CreateTableResponse> tableCF = getCreateTableResponseCompletableFuture(clientdB);

        CreateTableResponse tableResponse = tableCF.join();
        log.info("Setting up table with response {}", tableResponse);
    }

    private static CompletableFuture<CreateTableResponse> getCreateTableResponseCompletableFuture(DynamoDbAsyncClient clientdB) {
        return clientdB.createTable(
                CreateTableRequest.builder()
                        .tableName("Products")
                        .attributeDefinitions(
                                AttributeDefinition.builder()
                                        .attributeName("id")
                                        .attributeType(ScalarAttributeType.S)
                                        .build(),
                                AttributeDefinition.builder()
                                        .attributeName("code")
                                        .attributeType(ScalarAttributeType.S)
                                        .build())
                        .keySchema(KeySchemaElement.builder()
                                .attributeName("id")
                                .keyType(KeyType.HASH)
                                .build())
                        .tableName("products")
                        .billingMode(BillingMode.PROVISIONED)
                        .provisionedThroughput(ProvisionedThroughput.builder()
                                .readCapacityUnits(1L)
                                .writeCapacityUnits(1L)
                                .build())
                        .globalSecondaryIndexes(GlobalSecondaryIndex.builder()
                                .indexName("codeIdx")
                                .keySchema(KeySchemaElement.builder()
                                        .attributeName("code")
                                        .keyType(KeyType.HASH)
                                        .build())
                                .projection(Projection.builder()
                                        .projectionType(ProjectionType.KEYS_ONLY)
                                        .build())
                                .provisionedThroughput(ProvisionedThroughput.builder()
                                        .readCapacityUnits(1L)
                                        .writeCapacityUnits(1L)
                                        .build())
                                .build())
                        .build());
    }
}
