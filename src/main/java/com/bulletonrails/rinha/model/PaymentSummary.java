package com.bulletonrails.rinha.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record PaymentSummary(
        @JsonProperty("default") ProcessorSummary defaultProcessor,
        @JsonProperty("fallback") ProcessorSummary fallbackProcessor
) {
    public static PaymentSummary empty() {
        return new PaymentSummary(
                new ProcessorSummary(0, BigDecimal.ZERO),
                new ProcessorSummary(0, BigDecimal.ZERO)
        );
    }

    public PaymentSummary add(PaymentSummary other) {
        return new PaymentSummary(
                defaultProcessor.add(other.defaultProcessor),
                fallbackProcessor.add(other.fallbackProcessor)
        );
    }
}

