package br.com.murillo.auditservice.products.services;

import br.com.murillo.auditservice.events.dtos.ProductEventType;
import br.com.murillo.auditservice.events.dtos.ProductFailureEventDTO;
import br.com.murillo.auditservice.events.dtos.SnsMessageDTO;
import br.com.murillo.auditservice.products.repositories.ProductFailureEventRepository;
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
public class ProductFailureEventsConsumer {
    private static final Logger LOG = LogManager.getLogger(ProductFailureEventsConsumer.class);
    private final ObjectMapper objectMapper;
    private final SqsAsyncClient sqsAsyncClient;
    private final String productFailureEventsQueueUrl;
    private final ReceiveMessageRequest receiveMessageRequest;
    private final ProductFailureEventRepository productFailureEventRepository;

    @Autowired
    public ProductFailureEventsConsumer(ObjectMapper objectMapper, SqsAsyncClient sqsAsyncClient,
                                        @Value("${aws.sqs.queue.product.failure.events.url}") String productFailureEventsQueueUrl,
                                        ProductFailureEventRepository productFailureEventRepository) {
        this.objectMapper = objectMapper;
        this.sqsAsyncClient = sqsAsyncClient;
        this.productFailureEventsQueueUrl = productFailureEventsQueueUrl;
        this.receiveMessageRequest = ReceiveMessageRequest.builder()
                .maxNumberOfMessages(10)
                .queueUrl(productFailureEventsQueueUrl)
                .build();
        this.productFailureEventRepository = productFailureEventRepository;
    }

    @Scheduled(fixedDelay = 5000)
    public void receiveProductFailureEventsMessages() {
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

                    if (ProductEventType.PRODUCT_FAILURE == eventType) {
                        ProductFailureEventDTO productFailureEventDTO = objectMapper
                                .readValue(snsMessageDTO.message(), ProductFailureEventDTO.class);
                        LOG.info("Product failure event: {} - Id: {} ", eventType, productFailureEventDTO.id());
                    } else {
                        LOG.error("Invalid product failure event: {} ", eventType);
                        throw new Exception("Invalid product failure event");
                    }

                    sqsAsyncClient.deleteMessage(DeleteMessageRequest.builder()
                            .queueUrl(productFailureEventsQueueUrl)
                            .receiptHandle(message.receiptHandle())
                            .build()).join();
                    LOG.info("Message deleted...");
                } catch (Exception e) {
                    LOG.error("Failed to parse product failure event message");
                    throw new RuntimeException(e);
                } finally {
                    ThreadContext.clearAll();
                }
            });
        }
    }
}