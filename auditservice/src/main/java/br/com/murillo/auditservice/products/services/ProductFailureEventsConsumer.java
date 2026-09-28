package br.com.murillo.auditservice.products.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

@Service
public class ProductFailureEventsConsumer {
    private static final Logger LOG = LogManager.getLogger(ProductFailureEventsConsumer.class);
    private final ObjectMapper objectMapper;
    private final SqsAsyncClient sqsAsyncClient;
    private final String productFailureEventsQueueUrl;
    private final ReceiveMessageRequest receiveMessageRequest;

    @Autowired
    public ProductFailureEventsConsumer(ObjectMapper objectMapper, SqsAsyncClient sqsAsyncClient,
                                 @Value("${aws.sqs.queue.product.failure.events.url}") String productFailureEventsQueueUrl) {
        this.objectMapper = objectMapper;
        this.sqsAsyncClient = sqsAsyncClient;
        this.productFailureEventsQueueUrl = productFailureEventsQueueUrl;
        this.receiveMessageRequest = ReceiveMessageRequest.builder()
                .maxNumberOfMessages(10)
                .queueUrl(productFailureEventsQueueUrl)
                .build();
    }
}