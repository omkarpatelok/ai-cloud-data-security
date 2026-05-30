package com.security.cloudscanner.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AiDetectionService {

    private final RestTemplate restTemplate;
    private final AiServiceResilienceSupport resilienceSupport;
    private final String classifierUrl;

    public AiDetectionService(
            RestTemplate restTemplate,
            AiServiceResilienceSupport resilienceSupport,
            @Value("${ai.classifier.url:http://localhost:9100/classify}") String classifierUrl
    ) {
        this.restTemplate = restTemplate;
        this.resilienceSupport = resilienceSupport;
        this.classifierUrl = classifierUrl;
    }

    public List<String> classify(String content) {
        List<String> predictions = new ArrayList<>();
        if (content == null || content.isBlank()) {
            return predictions;
        }

        return resilienceSupport.execute("bert-classifier", () -> {
            RequestEntity<Map<String, String>> request = RequestEntity
                    .post(URI.create(classifierUrl))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("text", content));

            Map<String, Object> response = restTemplate.exchange(
                    request,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            ).getBody();

            if (response == null) {
                return predictions;
            }

            Object predictionObject = response.get("prediction");
            if (predictionObject instanceof List<?> predictionList) {
                for (Object item : predictionList) {
                    predictions.add(String.valueOf(item));
                }
            } else if (predictionObject != null) {
                predictions.add(String.valueOf(predictionObject));
            }
            return predictions;
        }, () -> {
            System.out.println("AI classifier unavailable, continuing without AI findings");
            return predictions;
        });
    }

    public boolean hasSensitiveSignal(List<String> predictions) {
        for (String prediction : predictions) {
            String normalized = prediction.toUpperCase();
            if (normalized.contains("NEGATIVE")
                    || normalized.contains("SENSITIVE")
                    || normalized.contains("LABEL_1")
                    || normalized.contains("CONFIDENTIAL_DOCUMENT")
                    || normalized.contains("PAYMENT_INFORMATION")
                    || normalized.contains("INTERNAL_DATA")) {
                return true;
            }
        }
        return false;
    }
}
