package br.com.murillo.auditservice.products.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

public record ProductEventApiPageDTO(List<ProductEventApiDTO> items,
                                     @JsonInclude(JsonInclude.Include.NON_NULL) String lastEvaluatedTimestamp,
                                     int count) {}