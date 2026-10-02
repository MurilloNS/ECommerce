package br.com.murillo.auditservice.products.dtos;

import br.com.murillo.auditservice.products.models.ProductEvent;

public record ProductEventApiDTO(String productId, String code, float price, String requestId, String email, long createdAt) {
    public ProductEventApiDTO(ProductEvent productEvent) {
        this(productEvent.getInfo().getId(), productEvent.getInfo().getCode(), productEvent.getInfo().getPrice(),
                productEvent.getInfo().getRequestId(), productEvent.getEmail(), productEvent.getCreatedAt());
    }
}