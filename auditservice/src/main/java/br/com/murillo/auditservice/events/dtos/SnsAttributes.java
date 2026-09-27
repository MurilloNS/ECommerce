package br.com.murillo.auditservice.events.dtos;

public record SnsAttributes(SnsMessageAttribute traceId, SnsMessageAttribute eventType, SnsMessageAttribute requestId) {}