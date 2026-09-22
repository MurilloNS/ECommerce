package br.com.murillo.productsservice.events.services;

import br.com.murillo.productsservice.events.dtos.EventType;
import br.com.murillo.productsservice.events.dtos.ProductEventDTO;
import br.com.murillo.productsservice.products.models.Product;
import com.amazonaws.xray.AWSXRay;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import software.amazon.awssdk.services.sns.model.Topic;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Service
public class EventsPublisher {
    private final SnsAsyncClient snsAsyncClient;
    private final Topic productEventsTopic;
    private final ObjectMapper objectMapper;

    @Autowired
    public EventsPublisher(SnsAsyncClient snsAsyncClient, @Qualifier("productEventsTopic") Topic productEventsTopic,
                           ObjectMapper objectMapper) {
        this.snsAsyncClient = snsAsyncClient;
        this.productEventsTopic = productEventsTopic;
        this.objectMapper = objectMapper;
    }

    public CompletableFuture<PublishResponse> sendProductEvent(Product product, EventType eventType, String email)
            throws JsonProcessingException {
        ProductEventDTO productEventDTO = new ProductEventDTO(product.getId(), product.getCode(), email, product.getPrice());
        return sendEvent(objectMapper.writeValueAsString(productEventDTO), eventType);
    }

    private CompletableFuture<PublishResponse> sendEvent(String data, EventType eventType) {
        return snsAsyncClient.publish(PublishRequest.builder()
                .message(data)
                .messageAttributes(Map.of(
                        "eventType", MessageAttributeValue.builder()
                                .dataType("String")
                                .stringValue(eventType.name())
                                .build(),
                        "requestId", MessageAttributeValue.builder()
                                .dataType("String")
                                .stringValue(ThreadContext.get("requestId"))
                                .build(),
                        "traceId", MessageAttributeValue.builder()
                                .dataType("String")
                                .stringValue(Objects.requireNonNull(
                                        AWSXRay.getCurrentSegment()).getTraceId().toString())
                                .build()))
                .topicArn(productEventsTopic.topicArn())
                .build());
    }
}