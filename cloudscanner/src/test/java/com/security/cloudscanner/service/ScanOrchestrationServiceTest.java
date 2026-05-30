package com.security.cloudscanner.service;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScanOrchestrationServiceTest {

    @Test
    void extractsDecodedObjectKeysFromS3EventPayload() {
        ScanOrchestrationService service = new ScanOrchestrationService(
                null, null, null, null, null, null, null
        );

        Map<String, Object> payload = Map.of(
                "Records", List.of(
                        Map.of(
                                "s3", Map.of(
                                        "object", Map.of(
                                                "key", "folder%2Fcustomer+report.txt"
                                        )
                                )
                        )
                )
        );

        List<String> objectKeys = service.extractObjectKeys(payload);

        assertEquals(List.of("folder/customer report.txt"), objectKeys);
    }
}
