package com.security.cloudscanner.audit;

import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class AuditService {

    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    public void log(String resourceName, String action, String reason) {

        AuditLog log = new AuditLog();
        log.setResourceName(resourceName);
        log.setAction(action);
        log.setReason(reason);
        log.setTimestamp(LocalDateTime.now());

        repository.save(log);

        // Alert (console-based for now)
        if ("BLOCK".equals(action)) {
            System.out.println("🚨 ALERT: BLOCKED resource -> " + resourceName);
        }
    }
}