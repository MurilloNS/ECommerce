package br.com.murillo.invoicesservice.invoices.repositories;

import br.com.murillo.invoicesservice.invoices.dtos.InvoiceFileDTO;
import br.com.murillo.invoicesservice.invoices.models.Invoice;
import br.com.murillo.invoicesservice.invoices.models.InvoiceProduct;
import com.amazonaws.xray.spring.aop.XRayEnabled;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

@Repository
@XRayEnabled
public class InvoiceRepository {
    private static final String PARTITION_KEY = "#invoice_";
    private final DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;
    private final DynamoDbAsyncTable<Invoice> invoiceTable;

    public InvoiceRepository(@Value("${invoices.ddb.name}") String invoicesDdbName,
                                             DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient) {
        this.dynamoDbEnhancedAsyncClient = dynamoDbEnhancedAsyncClient;
        this.invoiceTable = dynamoDbEnhancedAsyncClient.table(invoicesDdbName, TableSchema.fromBean(Invoice.class));
    }

    public CompletableFuture<Void> createInvoice(InvoiceFileDTO invoiceFileDTO, String invoiceTransactionId,
                                                 String invoiceFileTransactionId) {
        long timestamp = Instant.now().toEpochMilli();

        Invoice invoice = new Invoice();
        invoice.setPk(PARTITION_KEY.concat(invoiceFileDTO.customerEmail()));
        invoice.setSk(invoiceFileDTO.invoiceNumber());
        invoice.setTotalValue(invoiceFileDTO.totalValue());
        invoice.setProducts(invoiceFileDTO.products().stream().map(invoiceProductFileDTO -> {
            InvoiceProduct invoiceProduct = new InvoiceProduct();
            invoiceProduct.setId(invoiceProductFileDTO.id());
            invoiceProduct.setQuantity(invoiceProductFileDTO.quantity());

            return invoiceProduct;
        }).toList());
        invoice.setInvoiceTransactionId(invoiceTransactionId);
        invoice.setFileTransactionId(invoiceFileTransactionId);
        invoice.setCreatedAt(timestamp);
        invoice.setTtl(0L);

        return invoiceTable.putItem(invoice);
    }

    public SdkPublisher<Page<Invoice>> findByCustomerEmail(String email) {
        String pk = PARTITION_KEY.concat(email);

        return invoiceTable.query(QueryEnhancedRequest.builder()
                .queryConditional(QueryConditional.keyEqualTo(Key.builder()
                        .partitionValue(pk)
                        .build()))
                .build());
    }
}