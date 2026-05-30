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
public class NlpDetectionService {

    private final RestTemplate restTemplate;
    private final AiServiceResilienceSupport resilienceSupport;
    private final String nlpServiceUrl;

    public NlpDetectionService(
            RestTemplate restTemplate,
            AiServiceResilienceSupport resilienceSupport,
            @Value("${ai.nlp.url:http://localhost:9000/detect}") String nlpServiceUrl
    ) {
        this.restTemplate = restTemplate;
        this.resilienceSupport = resilienceSupport;
        this.nlpServiceUrl = nlpServiceUrl;
    }

    public List<String> detect(String content) {
        List<String> findings = new ArrayList<>();
        if (content == null || content.isBlank()) {
            return findings;
        }

        return resilienceSupport.execute("nlp-service", () -> {
            RequestEntity<Map<String, String>> request = RequestEntity
                    .post(URI.create(nlpServiceUrl))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("text", content));

            Map<String, Object> response = restTemplate.exchange(
                    request,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            ).getBody();

            if (response == null) {
                return findings;
            }

            Object entitiesObject = response.get("entities");
            if (entitiesObject instanceof List<?> entities) {
                for (Object entityObject : entities) {
                    if (entityObject instanceof Map<?, ?> entity) {
                        Object text = entity.get("text");
                        Object label = entity.get("label");
                        if (text != null && label != null) {
                            findings.add("NLP:" + label + ":" + text);
                        }
                    }
                }
            }
            return findings;
        }, () -> {
            System.out.println("NLP service unavailable, continuing without NLP findings");
            return findings;
        });
    }
}
