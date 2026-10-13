package br.com.murillo.invoicesservice.invoices.services;

import br.com.murillo.invoicesservice.invoices.repositories.InvoiceFileTransactionsRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

@Service
public class S3InvoicesConsumer {
    private static final Logger LOG = LogManager.getLogger(S3InvoicesConsumer.class);
    private final S3AsyncClient s3AsyncClient;
    private final InvoiceFileTransactionsRepository invoiceFileTransactionsRepository;
    private final ObjectMapper objectMapper;
    private final SqsAsyncClient sqsAsyncClient;
    private final String invoiceEventsQueueUrl;
    private final ReceiveMessageRequest receiveMessageRequest;

    @Autowired
    public S3InvoicesConsumer(S3AsyncClient s3AsyncClient,
                              InvoiceFileTransactionsRepository invoiceFileTransactionsRepository,
                              ObjectMapper objectMapper, SqsAsyncClient sqsAsyncClient,
                              @Value("${aws.sqs.queue.invoices.events.url}") String invoiceEventsQueueUrl) {
        this.s3AsyncClient = s3AsyncClient;
        this.invoiceFileTransactionsRepository = invoiceFileTransactionsRepository;
        this.objectMapper = objectMapper;
        this.sqsAsyncClient = sqsAsyncClient;
        this.invoiceEventsQueueUrl = invoiceEventsQueueUrl;
        this.receiveMessageRequest = ReceiveMessageRequest.builder()
                .maxNumberOfMessages(5)
                .queueUrl(invoiceEventsQueueUrl)
                .build();
    }

    private void deleteMessage(Message message) {
        sqsAsyncClient.deleteMessage(DeleteMessageRequest.builder()
                .queueUrl(invoiceEventsQueueUrl)
                .receiptHandle(message.receiptHandle())
                .build()).join();

        LOG.info("Messages deleted...");
    }
}