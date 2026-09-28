package br.com.murillo.auditservice.products.services;

import br.com.murillo.auditservice.events.dtos.ProductEventDTO;
import br.com.murillo.auditservice.events.dtos.ProductEventType;
import br.com.murillo.auditservice.events.dtos.SnsMessageDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.util.List;

@Service
public class ProductEventsConsumer {
    private static final Logger LOG = LogManager.getLogger(ProductEventsConsumer.class);
    private final ObjectMapper objectMapper;
    private final SqsAsyncClient sqsAsyncClient;
    private final String productEventsQueueUrl;
    private final ReceiveMessageRequest receiveMessageRequest;

    @Autowired
    public ProductEventsConsumer(ObjectMapper objectMapper, SqsAsyncClient sqsAsyncClient,
                                 @Value("${aws.sqs.queue.product.events.url}") String productEventsQueueUrl) {
        this.objectMapper = objectMapper;
        this.sqsAsyncClient = sqsAsyncClient;
        this.productEventsQueueUrl = productEventsQueueUrl;
        this.receiveMessageRequest = ReceiveMessageRequest.builder()
                .maxNumberOfMessages(5)
                .queueUrl(productEventsQueueUrl)
                .build();
    }

    @Scheduled(fixedDelay = 1000)
    public void receiveProductEventsMessages() {
        List<Message> messages;
        while (!(messages = sqsAsyncClient.receiveMessage(receiveMessageRequest).join().messages()).isEmpty()) {
            LOG.info("Reading {} messages ", messages.size());
            messages.parallelStream().forEach(message -> {
                try {
                    SnsMessageDTO snsMessageDTO = objectMapper.readValue(message.body(), SnsMessageDTO.class);
                    ThreadContext.put("messageId", snsMessageDTO.messageId());
                    ThreadContext.put("requestId", snsMessageDTO.messageAttributes().requestId().value());
                    ProductEventType eventType = ProductEventType.
                            valueOf(snsMessageDTO.messageAttributes().eventType().value());

                    switch (eventType) {
                        case PRODUCT_CREATED, PRODUCT_UPDATED, PRODUCT_DELETED -> {
                            ProductEventDTO productEventDTO = objectMapper
                                    .readValue(snsMessageDTO.message(), ProductEventDTO.class);
                            LOG.info("Product event: {} - Id: {} ", eventType, productEventDTO.id());
                        }
                        default -> {
                            LOG.error("Invalid product event: {} ", eventType);
                            throw new Exception("Invalid product event");
                        }
                    }

                    sqsAsyncClient.deleteMessage(DeleteMessageRequest.builder()
                            .queueUrl(productEventsQueueUrl)
                            .receiptHandle(message.receiptHandle())
                            .build()).join();
                    LOG.info("Message deleted...");
                } catch (Exception e) {
                    LOG.error("Failed to parse product event message");
                    throw new RuntimeException(e);
                } finally {
                    ThreadContext.clearAll();
                }
            });
        }
    }
}