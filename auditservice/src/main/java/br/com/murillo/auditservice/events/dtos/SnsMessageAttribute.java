package br.com.murillo.auditservice.events.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SnsMessageAttribute(@JsonProperty("Type") String type, @JsonProperty("Value") String value) {}