package br.com.murillo.invoicesservice.invoices.controllers;

import br.com.murillo.invoicesservice.invoices.repositories.InvoiceFileTransactionsRepository;
import br.com.murillo.invoicesservice.invoices.services.S3InvoicesService;
import com.amazonaws.xray.spring.aop.XRayEnabled;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@XRayEnabled
@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {
    private static final Logger LOG = LogManager.getLogger(InvoiceController.class);
    private final S3InvoicesService s3InvoicesService;
    private final InvoiceFileTransactionsRepository invoiceFileTransactionsRepository;

    @Autowired
    public InvoiceController(S3InvoicesService s3InvoicesService, InvoiceFileTransactionsRepository invoiceFileTransactionsRepository) {
        this.s3InvoicesService = s3InvoicesService;
        this.invoiceFileTransactionsRepository = invoiceFileTransactionsRepository;
    }
}