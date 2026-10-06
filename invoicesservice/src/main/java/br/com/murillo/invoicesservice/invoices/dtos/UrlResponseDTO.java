package br.com.murillo.invoicesservice.invoices.dtos;

public record UrlResponseDTO(String url, int expireIn, String transactionId) {}