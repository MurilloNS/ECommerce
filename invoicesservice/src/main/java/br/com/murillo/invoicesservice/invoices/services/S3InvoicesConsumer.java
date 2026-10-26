package br.com.murillo.invoicesservice.invoices.services;

import br.com.murillo.invoicesservice.invoices.dtos.InvoiceFileDTO;
import br.com.murillo.invoicesservice.invoices.enums.InvoiceTransactionStatus;
import br.com.murillo.invoicesservice.invoices.models.InvoiceFileTransaction;
import br.com.murillo.invoicesservice.invoices.repositories.InvoiceFileTransactionsRepository;
import br.com.murillo.invoicesservice.invoices.repositories.InvoiceRepository;
import br.com.murillo.invoicesservice.invoices.repositories.InvoiceTransactionRepository;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import com.amazonaws.services.lambda.runtime.serialization.PojoSerializer;
import com.amazonaws.services.lambda.runtime.serialization.events.LambdaEventSerializers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static br.com.murillo.invoicesservice.invoices.enums.InvoiceFileTransactionStatus.*;

@Service
public class S3InvoicesConsumer {
    private static final Logger LOG = LogManager.getLogger(S3InvoicesConsumer.class);
    private final S3AsyncClient s3AsyncClient;
    private final InvoiceFileTransactionsRepository invoiceFileTransactionsRepository;
    private final InvoiceTransactionRepository invoiceTransactionRepository;
    private final InvoiceRepository invoiceRepository;
    private final ObjectMapper objectMapper;
    private final SqsAsyncClient sqsAsyncClient;
    private final String invoiceEventsQueueUrl;
    private final ReceiveMessageRequest receiveMessageRequest;

    @Autowired
    public S3InvoicesConsumer(S3AsyncClient s3AsyncClient,
                              InvoiceFileTransactionsRepository invoiceFileTransactionsRepository,
                              InvoiceTransactionRepository invoiceTransactionRepository,
                              InvoiceRepository invoiceRepository,
                              ObjectMapper objectMapper, SqsAsyncClient sqsAsyncClient,
                              @Value("${aws.sqs.queue.invoices.events.url}") String invoiceEventsQueueUrl) {
        this.s3AsyncClient = s3AsyncClient;
        this.invoiceFileTransactionsRepository = invoiceFileTransactionsRepository;
        this.invoiceTransactionRepository = invoiceTransactionRepository;
        this.invoiceRepository = invoiceRepository;
        this.objectMapper = objectMapper;
        this.sqsAsyncClient = sqsAsyncClient;
        this.invoiceEventsQueueUrl = invoiceEventsQueueUrl;
        this.receiveMessageRequest = ReceiveMessageRequest.builder()
                .maxNumberOfMessages(5)
                .queueUrl(invoiceEventsQueueUrl)
                .build();
    }

    @Scheduled(fixedDelay = 1000)
    public void receiveInvoiceEventsMessage() {
        List<Message> messages;

        while (!(messages = sqsAsyncClient.receiveMessage(receiveMessageRequest).join().messages()).isEmpty()) {
            AtomicBoolean allInvoicesProcessed = new AtomicBoolean(true);
            LOG.info("Reading messages: {}", messages.size());

            messages.parallelStream().forEach(message -> {
                S3EventNotification eventNotification;
                LOG.info("Parsing S3 event message");
                PojoSerializer<S3EventNotification> s3EventNotificationPojoSerializer =
                        LambdaEventSerializers.serializerFor(S3EventNotification.class,
                                ClassLoader.getSystemClassLoader());
                eventNotification = s3EventNotificationPojoSerializer.fromJson(message.body());
                if (eventNotification == null || eventNotification.getRecords().isEmpty()) {
                    LOG.error("Failed to parse S3 event notification");
                    deleteMessage(message);
                    return;
                }
                LOG.info("Number of record: {}", eventNotification.getRecords().size());

                List<CompletableFuture<Boolean>> futures = new ArrayList<>();
                eventNotification.getRecords().parallelStream().forEach(s3EventNotificationRecord -> {
                    String key = s3EventNotificationRecord.getS3().getObject().getKey();
                    ThreadContext.put("invoiceFileTransactionId", key);
                    LOG.info("Invoice file transactionId: {}", key);

                    futures.add(processRecord(s3EventNotificationRecord));

                    ThreadContext.clearAll();
                });

                CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
                allInvoicesProcessed.set(futures.stream().allMatch(CompletableFuture::join));

                LOG.info("All records were processed...");

                deleteMessage(message);

                if (allInvoicesProcessed.get()) {
                    LOG.info("All invoices processed");
                } else {
                    LOG.error("Some invoice file was not treated.");
                }

                LOG.info("Finish...");
            });
        }
    }

    private CompletableFuture<Boolean> processRecord(S3EventNotification.S3EventNotificationRecord record) {
        String key = record.getS3().getObject().getKey();
        LOG.info("Start processing the record - key:");
        String bucketName = record.getS3().getBucket().getName();

        // Get the invoice file transaction
        InvoiceFileTransaction invoiceFileTransaction = invoiceFileTransactionsRepository
                .getInvoiceFileTransaction(key).join();
        if ((invoiceFileTransaction == null) || (!invoiceFileTransaction.getFileTransactionStatus().name()
                .equals(GENERATED.name()))) {
            LOG.error("Invoice file transaction not found or non valid transaction status - key: {}", key);
            return CompletableFuture.supplyAsync(() -> false);
        }

        // Get the S3 object (invoice file)
        LOG.info("Reading invoice file from S3 bucket...");
        ResponseInputStream<GetObjectResponse> s3Object = this.s3AsyncClient.getObject(GetObjectRequest.builder()
                .key(key)
                .bucket(bucketName)
                .build(), AsyncResponseTransformer.toBlockingInputStream()).join();

        // Update the invoice file transaction status
        invoiceFileTransactionsRepository.updateInvoiceFileTransactionStatus(key, FILE_RECEIVED).join();

        int invoiceCount = 0;
        try (s3Object; BufferedReader reader = new BufferedReader(new InputStreamReader(s3Object))) {
            // Process each invoice from the file (also update the invoice transaction status)
            String line;
            while((line = reader.readLine()) != null) {
                processInvoice(line, invoiceFileTransaction).join();
                LOG.info("Invoice processed...");
                invoiceCount++;
            }

            // Delete the object from the S3 bucket
            LOG.info("Deleting the invoice file from S3...");
            CompletableFuture<DeleteObjectResponse> deleteObjectResponseCompletableFuture =
                    s3AsyncClient.deleteObject(DeleteObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .build());

            // Update the invoice file transaction status
            CompletableFuture<InvoiceFileTransaction> invoiceFileTransactionCompletableFuture =
                    invoiceFileTransactionsRepository.updateInvoiceFileTransactionStatus(key, FILE_PROCESSED);

            CompletableFuture
                    .allOf(deleteObjectResponseCompletableFuture, invoiceFileTransactionCompletableFuture).join();
        } catch (IOException e) {
            LOG.error("Failed to read invoice file");
            invoiceFileTransactionsRepository.updateInvoiceFileTransactionStatus(key, ERROR).join();

            return CompletableFuture.supplyAsync(() -> false);
        }

        LOG.info("Number of invoices processed: {}", invoiceCount);

        return CompletableFuture.supplyAsync(() -> true);
    }

    private CompletableFuture<Boolean> processInvoice(String line, InvoiceFileTransaction invoiceFileTransaction)
        throws JsonProcessingException {
        InvoiceTransactionStatus invoiceTransactionStatus;
        String invoiceFileTransactionId = invoiceFileTransaction.getSk();
        LOG.info("Persisting the invoice...");

        String invoiceTransactionId = UUID.randomUUID().toString();
        InvoiceFileDTO invoiceFileDTO = objectMapper.readValue(line, InvoiceFileDTO.class);
        if (invoiceFileDTO.products().size() == 0) {
            LOG.error("Invoice import failed - empty products list");
            invoiceTransactionStatus = InvoiceTransactionStatus.EMPTY_PRODUCTS_LIST;
        } else {
            invoiceTransactionStatus = InvoiceTransactionStatus.OK;
        }

        CompletableFuture<Void> createInvoiceFuture = invoiceRepository
                .createInvoice(invoiceFileDTO, invoiceTransactionId, invoiceFileTransactionId);
        CompletableFuture<Void> createInvoiceTransactionFuture = invoiceTransactionRepository
                .createInvoiceTransaction(invoiceFileDTO.customerEmail(), invoiceFileDTO.invoiceNumber(),
                        invoiceTransactionId, invoiceFileTransactionId, invoiceTransactionStatus);

        CompletableFuture.allOf(createInvoiceFuture, createInvoiceTransactionFuture).join();
        LOG.info("Invoice persisted");

        return CompletableFuture.supplyAsync(() -> true);
    }

    private void deleteMessage(Message message) {
        sqsAsyncClient.deleteMessage(DeleteMessageRequest.builder()
                .queueUrl(invoiceEventsQueueUrl)
                .receiptHandle(message.receiptHandle())
                .build()).join();

        LOG.info("Messages deleted...");
    }
}