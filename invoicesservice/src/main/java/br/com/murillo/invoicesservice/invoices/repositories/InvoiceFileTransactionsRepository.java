package br.com.murillo.invoicesservice.invoices.repositories;

import br.com.murillo.invoicesservice.invoices.models.InvoiceFileTransaction;
import com.amazonaws.xray.spring.aop.XRayEnabled;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

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
}