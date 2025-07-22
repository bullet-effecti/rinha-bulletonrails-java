package com.bulletonrails.rinha.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record ProcessorSummary(
        @JsonProperty("totalRequests") long totalRequests,
        @JsonProperty("totalAmount") BigDecimal totalAmount
) {
    public ProcessorSummary add(ProcessorSummary other) {
        return new ProcessorSummary(
                totalRequests + other.totalRequests,
                totalAmount.add(other.totalAmount)
        );
    }
}
