package br.com.murillo.invoicesservice.invoices.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record InvoiceProductFileDTO(String id, int quantity) {}