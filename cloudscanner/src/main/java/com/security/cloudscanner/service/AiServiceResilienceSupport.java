package com.security.cloudscanner.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Component
public class AiServiceResilienceSupport {

    private final int maxRetries;
    private final int circuitFailureThreshold;
    private final Duration cooldownDuration;
    private final Map<String, CircuitState> circuits = new ConcurrentHashMap<>();

    public AiServiceResilienceSupport(
            @Value("${ai.resilience.max-retries:2}") int maxRetries,
            @Value("${ai.resilience.failure-threshold:3}") int circuitFailureThreshold,
            @Value("${ai.resilience.cooldown-seconds:30}") long cooldownSeconds
    ) {
        this.maxRetries = Math.max(1, maxRetries);
        this.circuitFailureThreshold = Math.max(1, circuitFailureThreshold);
        this.cooldownDuration = Duration.ofSeconds(Math.max(1, cooldownSeconds));
    }

    public <T> T execute(String clientName, Callable<T> action, Supplier<T> fallback) {
        CircuitState circuit = circuits.computeIfAbsent(clientName, key -> new CircuitState());

        if (circuit.isOpen()) {
            return fallback.get();
        }

        Exception lastException = null;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                T response = action.call();
                circuit.onSuccess();
                return response;
            } catch (Exception ex) {
                lastException = ex;
                circuit.onFailure(circuitFailureThreshold, cooldownDuration);
            }
        }

        if (lastException != null) {
            System.out.println("AI client " + clientName + " failed after retries: " + lastException.getMessage());
        }
        return fallback.get();
    }

    private static final class CircuitState {
        private int consecutiveFailures;
        private Instant openUntil;

        private synchronized boolean isOpen() {
            if (openUntil == null) {
                return false;
            }
            if (Instant.now().isAfter(openUntil)) {
                openUntil = null;
                consecutiveFailures = 0;
                return false;
            }
            return true;
        }

        private synchronized void onSuccess() {
            consecutiveFailures = 0;
            openUntil = null;
        }

        private synchronized void onFailure(int threshold, Duration cooldown) {
            consecutiveFailures++;
            if (consecutiveFailures >= threshold) {
                openUntil = Instant.now().plus(cooldown);
            }
        }
    }
}
