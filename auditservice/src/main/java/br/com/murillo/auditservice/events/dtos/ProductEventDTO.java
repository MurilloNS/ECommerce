package br.com.murillo.auditservice.events.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductEventDTO(String id, String code, String email, float price) {}