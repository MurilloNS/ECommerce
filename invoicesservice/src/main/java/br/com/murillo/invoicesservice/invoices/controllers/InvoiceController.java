package br.com.murillo.invoicesservice.invoices.controllers;

import br.com.murillo.invoicesservice.invoices.dtos.InvoiceApiDTO;
import br.com.murillo.invoicesservice.invoices.dtos.InvoiceFileTransactionApiDTO;
import br.com.murillo.invoicesservice.invoices.dtos.InvoiceProductApiDTO;
import br.com.murillo.invoicesservice.invoices.dtos.UrlResponseDTO;
import br.com.murillo.invoicesservice.invoices.models.InvoiceFileTransaction;
import br.com.murillo.invoicesservice.invoices.repositories.InvoiceFileTransactionsRepository;
import br.com.murillo.invoicesservice.invoices.repositories.InvoiceRepository;
import br.com.murillo.invoicesservice.invoices.services.S3InvoicesService;
import com.amazonaws.xray.spring.aop.XRayEnabled;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@XRayEnabled
@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {
    private static final Logger LOG = LogManager.getLogger(InvoiceController.class);
    private final S3InvoicesService s3InvoicesService;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceFileTransactionsRepository invoiceFileTransactionsRepository;

    @Autowired
    public InvoiceController(S3InvoicesService s3InvoicesService, InvoiceRepository invoiceRepository,
                             InvoiceFileTransactionsRepository invoiceFileTransactionsRepository) {
        this.s3InvoicesService = s3InvoicesService;
        this.invoiceRepository = invoiceRepository;
        this.invoiceFileTransactionsRepository = invoiceFileTransactionsRepository;
    }

    @GetMapping
    public ResponseEntity<List<InvoiceApiDTO>> getAllInvoicesByEmail(@RequestParam String email) {
        LOG.info("Get all invoices by email");
        List<InvoiceApiDTO> invoicesApiDTO = new ArrayList<>();

        invoiceRepository.findByCustomerEmail(email).subscribe(invoicePage -> {
            invoicesApiDTO.addAll(invoicePage.items().parallelStream().map(invoice -> new InvoiceApiDTO(
                    invoice.getPk().split("_")[1],
                    invoice.getSk(),
                    invoice.getTotalValue(),
                    invoice.getProducts().parallelStream().map(invoiceProduct -> new InvoiceProductApiDTO(
                            invoiceProduct.getId(),
                            invoiceProduct.getQuantity()
                    )).toList(),
                    invoice.getInvoiceTransactionId(),
                    invoice.getFileTransactionId(),
                    invoice.getCreatedAt()
            )).toList());
        }).join();

        return new ResponseEntity<>(invoicesApiDTO, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<UrlResponseDTO> generatePreSignedUrl(@RequestHeader("requestId") String requestId) {
        String key = UUID.randomUUID().toString();
        int expiresIn = 300;

        ThreadContext.put("invoiceFileTransactionId", key);

        String preSignedUrl = s3InvoicesService.generatePreSignedUrl(key, expiresIn);

        invoiceFileTransactionsRepository.createInvoiceFileTransaction(key, requestId, expiresIn).join();

        LOG.info("Invoice file transaction generated...");

        return new ResponseEntity<>(new UrlResponseDTO(preSignedUrl, expiresIn, key), HttpStatus.OK);
    }

    @GetMapping("/transactions/{fileTransactionId}")
    public ResponseEntity<?> getInvoiceFileTransaction(@PathVariable String fileTransactionId) {
        LOG.info("Get invoice file transaction by its id: {}", fileTransactionId);

        InvoiceFileTransaction invoiceFileTransaction = invoiceFileTransactionsRepository
                .getInvoiceFileTransaction(fileTransactionId).join();
        if (invoiceFileTransaction != null) {
            return new ResponseEntity<>(new InvoiceFileTransactionApiDTO(
                    fileTransactionId, invoiceFileTransaction.getFileTransactionStatus().name()), HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Invoice file transaction not found", HttpStatus.NOT_FOUND);
        }
    }
}