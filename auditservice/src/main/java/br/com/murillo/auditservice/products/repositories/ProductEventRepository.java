package br.com.murillo.auditservice.products.repositories;

import br.com.murillo.auditservice.events.dtos.ProductEventDTO;
import br.com.murillo.auditservice.events.dtos.ProductEventType;
import br.com.murillo.auditservice.products.models.ProductEvent;
import br.com.murillo.auditservice.products.models.ProductInfoEvent;
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
public class ProductEventRepository {
    private static final Logger LOG = LogManager.getLogger(ProductEventRepository.class);
    private final DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;
    private final DynamoDbAsyncTable<ProductEvent> productEventTable;

    @Autowired
    public ProductEventRepository(@Value("${aws.events.ddb}") String eventsDdbName,
                                  DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient) {
        this.dynamoDbEnhancedAsyncClient = dynamoDbEnhancedAsyncClient;
        this.productEventTable = dynamoDbEnhancedAsyncClient
                .table(eventsDdbName, TableSchema.fromBean(ProductEvent.class));
    }

    public CompletableFuture<Void> create(ProductEventDTO productEventDTO, ProductEventType productEventType,
                                          String messageId, String requestId, String traceId) {
        long timestamp = Instant.now().toEpochMilli();
        long ttl = Instant.now().plusSeconds(300).getEpochSecond();

        ProductEvent productEvent = new ProductEvent();
        productEvent.setPk("#product_".concat(productEventType.name()));
        productEvent.setSk(String.valueOf(timestamp));
        productEvent.setCreatedAt(timestamp);
        productEvent.setTtl(ttl);
        productEvent.setEmail(productEventDTO.email());

        ProductInfoEvent productInfoEvent = new ProductInfoEvent();
        productInfoEvent.setId(productEventDTO.id());
        productInfoEvent.setCode(productEventDTO.code());
        productInfoEvent.setPrice(productEventDTO.price());
        productInfoEvent.setMessageId(messageId);
        productInfoEvent.setRequestId(requestId);
        productInfoEvent.setTraceId(traceId);

        productEvent.setInfo(productInfoEvent);
        return productEventTable.putItem(productEvent);
    }
}