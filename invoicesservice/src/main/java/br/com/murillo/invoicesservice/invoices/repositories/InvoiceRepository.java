package br.com.murillo.invoicesservice.invoices.repositories;

import br.com.murillo.invoicesservice.invoices.models.Invoice;
import com.amazonaws.xray.spring.aop.XRayEnabled;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Repository
@XRayEnabled
public class InvoiceRepository {
    private static final String PARTITION_KEY = "#invoice_";
    private final DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;
    private final DynamoDbAsyncTable<Invoice> invoiceTable;

    public InvoiceRepository(@Value("${invoices.ddb.name}") String invoicesDdbName,
                                             DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient) {
        this.dynamoDbEnhancedAsyncClient = dynamoDbEnhancedAsyncClient;
        this.invoiceTable = dynamoDbEnhancedAsyncClient
                .table(invoicesDdbName, TableSchema.fromBean(Invoice.class));
    }
}