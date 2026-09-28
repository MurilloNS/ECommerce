package br.com.murillo.auditservice.events.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SnsMessageDTO(@JsonProperty("Message") String message, @JsonProperty("Type") String type,
                            @JsonProperty("TopicArn") String topicArn, @JsonProperty("Timestamp") String timestamp,
                            @JsonProperty("MessageId") String messageId,
                            @JsonProperty("MessageAttributes") SnsAttributes messageAttributes) {}