package com.security.cloudscanner.controller;

import com.security.cloudscanner.entity.ScanResult;
import com.security.cloudscanner.repository.ScanResultRepository;
import com.security.cloudscanner.service.ScanOrchestrationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/scans")
public class ScanController {

    private final ScanResultRepository repository;
    private final ScanOrchestrationService scanOrchestrationService;
    private final String s3EventToken;

    public ScanController(
            ScanResultRepository repository,
            ScanOrchestrationService scanOrchestrationService,
            @Value("${security.s3-event-token:}") String s3EventToken
    ) {
        this.repository = repository;
        this.scanOrchestrationService = scanOrchestrationService;
        this.s3EventToken = s3EventToken;
    }

    @PostMapping
    public ScanResult scanAndSave(@RequestBody ScanRequest request) {
        return scanOrchestrationService.scanResource(request.resourceName(), request.resourceType());
    }

    @PostMapping("/s3-event")
    public List<ScanResult> scanFromS3Event(
            @RequestBody Map<String, Object> eventPayload,
            @RequestHeader(value = "X-S3-Event-Token", required = false) String providedToken
    ) {
        validateS3EventToken(providedToken);
        return scanEventPayload(eventPayload, "S3_EVENT");
    }

    @PostMapping("/batch")
    public List<ScanResult> scanEntireBucket() {
        return scanOrchestrationService.scanEntireBucket();
    }

    @GetMapping
    public List<ScanResult> getAllScans() {
        return repository.findAll();
    }

    private void validateS3EventToken(String providedToken) {
        if (s3EventToken == null || s3EventToken.isBlank()) {
            return;
        }

        if (!s3EventToken.equals(providedToken)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid S3 event token");
        }
    }

    private List<ScanResult> scanEventPayload(Map<String, Object> eventPayload, String resourceType) {
        List<String> objectKeys = scanOrchestrationService.extractObjectKeys(eventPayload);
        if (objectKeys.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No S3 object keys found in event payload. Use a direct S3 event body like {\"Records\":[{\"s3\":{\"object\":{\"key\":\"file.txt\"}}}]}"
            );
        }

        List<ScanResult> results = new ArrayList<>();
        for (String objectKey : objectKeys) {
            results.add(scanOrchestrationService.scanResource(objectKey, resourceType));
        }
        return results;
    }

}
