package br.com.murillo.productsservice.products.repositories;

import br.com.murillo.productsservice.products.models.Product;
import com.amazonaws.xray.spring.aop.XRayEnabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.PagePublisher;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;

import java.util.concurrent.CompletableFuture;

@Repository
@XRayEnabled
public class ProductRepository {
    private final DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;
    private DynamoDbAsyncTable<Product> productsTable;

    @Autowired
    public ProductRepository(DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                             @Value("${aws.productsddb.name}") String productsDdbName) {
        this.dynamoDbEnhancedAsyncClient = dynamoDbEnhancedAsyncClient;
        productsTable = dynamoDbEnhancedAsyncClient.table(productsDdbName, TableSchema.fromBean(Product.class));
    }

    public PagePublisher<Product> getAll() {
        return productsTable.scan(); // Just for study
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

    public CompletableFuture<Product> update(Product product, String productId) {
        product.setId(productId);
        return productsTable.updateItem(UpdateItemEnhancedRequest.builder(Product.class)
                .item(product)
                .conditionExpression(Expression.builder()
                        .expression("attribute_exists(id)")
                        .build())
                .build());
    }
}