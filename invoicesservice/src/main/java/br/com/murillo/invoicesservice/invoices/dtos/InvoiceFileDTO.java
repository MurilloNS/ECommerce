package br.com.murillo.invoicesservice.invoices.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record InvoiceFileDTO(String customerEmail, String invoiceNumber, Float totalValue,
                             List<InvoiceProductFileDTO> products) {}