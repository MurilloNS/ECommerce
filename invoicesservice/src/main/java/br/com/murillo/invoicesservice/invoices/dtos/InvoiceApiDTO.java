package br.com.murillo.invoicesservice.invoices.dtos;

import java.util.List;

public record InvoiceApiDTO(String email, String invoiceNumber, Float totalValue, List<InvoiceProductApiDTO> products,
                            String invoiceTransactionId, String fileTransactionId, Long createdAt) {}