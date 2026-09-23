package br.com.murillo.productsservice.events.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;

public record ProductFailureEventDTO(String email, int status, String error,
                                     @JsonInclude(JsonInclude.Include.NON_NULL) String id) {}