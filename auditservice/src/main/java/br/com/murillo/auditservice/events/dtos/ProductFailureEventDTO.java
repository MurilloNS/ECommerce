package br.com.murillo.auditservice.events.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductFailureEventDTO(String email, int status, String error, String id) {}