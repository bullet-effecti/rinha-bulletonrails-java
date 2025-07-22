package com.bulletonrails.rinha.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record HealthStatus(
    @JsonProperty("failing") boolean failing,
    @JsonProperty("minResponseTime") int minResponseTime
) {
    public boolean isHealthy() {
        return !failing;
    }

    public int getScore() {
        // Melhor score = menor tempo de resposta + disponível
        if (failing) return Integer.MAX_VALUE;
        return minResponseTime;
    }
}
