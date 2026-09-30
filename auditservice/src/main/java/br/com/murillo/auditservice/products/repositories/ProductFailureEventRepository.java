package br.com.murillo.auditservice.products.repositories;

import br.com.murillo.auditservice.events.dtos.ProductEventType;
import br.com.murillo.auditservice.events.dtos.ProductFailureEventDTO;
import br.com.murillo.auditservice.products.models.ProductFailureEvent;
import br.com.murillo.auditservice.products.models.ProductInfoFailureEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

@Repository
public class ProductFailureEventRepository {
    private static final Logger LOG = LogManager.getLogger(ProductFailureEventRepository.class);
    private final DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;
    private final DynamoDbAsyncTable<ProductFailureEvent> productFailureEventTable;

    @Autowired
    public ProductFailureEventRepository(@Value("${aws.events.ddb}") String eventsDdbName,
                                  DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient) {
        this.dynamoDbEnhancedAsyncClient = dynamoDbEnhancedAsyncClient;
        this.productFailureEventTable = dynamoDbEnhancedAsyncClient
                .table(eventsDdbName, TableSchema.fromBean(ProductFailureEvent.class));
    }

    public CompletableFuture<Void> create(ProductFailureEventDTO productFailureEventDTO, ProductEventType productEventType,
                                          String messageId, String requestId, String traceId) {
        long timestamp = Instant.now().toEpochMilli();
        long ttl = Instant.now().plusSeconds(300).getEpochSecond();

        ProductFailureEvent productFailureEvent = new ProductFailureEvent();
        productFailureEvent.setPk("#product_".concat(productEventType.name()));
        productFailureEvent.setSk(String.valueOf(timestamp));
        productFailureEvent.setCreatedAt(timestamp);
        productFailureEvent.setTtl(ttl);
        productFailureEvent.setEmail(productFailureEventDTO.email());

        ProductInfoFailureEvent productInfoFailureEvent = new ProductInfoFailureEvent();
        productInfoFailureEvent.setId(productFailureEventDTO.id());
        productInfoFailureEvent.setMessageId(messageId);
        productInfoFailureEvent.setRequestId(requestId);
        productInfoFailureEvent.setTraceId(traceId);
        productInfoFailureEvent.setError(productFailureEventDTO.error());
        productInfoFailureEvent.setStatus(productFailureEventDTO.status());

        productFailureEvent.setInfo(productInfoFailureEvent);
        return productFailureEventTable.putItem(productFailureEvent);
    }
}