package br.com.murillo.invoicesservice.invoices.repositories;

import br.com.murillo.invoicesservice.invoices.enums.InvoiceFileTransactionStatus;
import br.com.murillo.invoicesservice.invoices.models.InvoiceFileTransaction;
import com.amazonaws.xray.spring.aop.XRayEnabled;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

@Repository
@XRayEnabled
public class InvoiceFileTransactionsRepository {
    private static final String PARTITION_KEY = "#fileTransaction";
    private final DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;
    private final DynamoDbAsyncTable<InvoiceFileTransaction> invoiceFileTransactionTable;

    public InvoiceFileTransactionsRepository(@Value("${invoices.ddb.name}") String invoicesDdbName,
                                             DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient) {
        this.dynamoDbEnhancedAsyncClient = dynamoDbEnhancedAsyncClient;
        this.invoiceFileTransactionTable = dynamoDbEnhancedAsyncClient
                .table(invoicesDdbName, TableSchema.fromBean(InvoiceFileTransaction.class));
    }

    public CompletableFuture<Void> createInvoiceFileTransaction(String transactionId, String requestId, int expiresIn) {
        long timestamp = Instant.now().toEpochMilli();
        long ttl = Instant.now().plusSeconds(300).getEpochSecond();

        InvoiceFileTransaction invoiceFileTransaction = new InvoiceFileTransaction();
        invoiceFileTransaction.setPk(PARTITION_KEY);
        invoiceFileTransaction.setSk(transactionId);
        invoiceFileTransaction.setTtl(ttl);
        invoiceFileTransaction.setRequestId(requestId);
        invoiceFileTransaction.setCreatedAt(timestamp);
        invoiceFileTransaction.setExpiresIn(expiresIn);
        invoiceFileTransaction.setFileTransactionStatus(InvoiceFileTransactionStatus.GENERATED);

        return invoiceFileTransactionTable.putItem(invoiceFileTransaction);
    }

    public CompletableFuture<InvoiceFileTransaction> updateInvoiceFileTransactionStatus(
            String transactionId, InvoiceFileTransactionStatus status) {
        InvoiceFileTransaction invoiceFileTransaction = new InvoiceFileTransaction();
        invoiceFileTransaction.setPk(PARTITION_KEY);
        invoiceFileTransaction.setSk(transactionId);
        invoiceFileTransaction.setFileTransactionStatus(status);

        return invoiceFileTransactionTable.updateItem(UpdateItemEnhancedRequest.builder(InvoiceFileTransaction.class)
                .item(invoiceFileTransaction)
                .ignoreNulls(true)
                .conditionExpression(Expression.builder()
                        .expression("attribute_exists(sk)")
                        .build())
                .build());
    }
}