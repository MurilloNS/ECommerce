package br.com.murillo.auditservice.products.repositories;

import br.com.murillo.auditservice.products.models.ProductEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Repository
public class ProductEventRepository {
    private static final Logger LOG = LogManager.getLogger(ProductEventRepository.class);
    private final DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;
    private final DynamoDbAsyncTable<ProductEvent> productEventTable;

    @Autowired
    public ProductEventRepository(@Value("${aws.events.ddb}") String eventsDdbName,
                                  DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient) {
        this.dynamoDbEnhancedAsyncClient = dynamoDbEnhancedAsyncClient;
        this.productEventTable = dynamoDbEnhancedAsyncClient.table(eventsDdbName, TableSchema.fromBean(ProductEvent.class));
    }
}