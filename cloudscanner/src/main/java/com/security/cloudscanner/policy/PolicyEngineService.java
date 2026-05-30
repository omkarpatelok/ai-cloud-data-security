package com.security.cloudscanner.policy;

import org.springframework.stereotype.Service;
@Service
public class PolicyEngineService {

    public PolicyDecisionResult evaluate(String dataType, String sensitivity) {

        if ("PII".equals(dataType) && "HIGH".equals(sensitivity)) {
            return new PolicyDecisionResult(
                "REVIEW",
                "Sensitive data detected – requires contextual risk evaluation"
            );
        }

        return new PolicyDecisionResult(
            "ALLOW",
            "No sensitive data detected"
        );
    }
}
