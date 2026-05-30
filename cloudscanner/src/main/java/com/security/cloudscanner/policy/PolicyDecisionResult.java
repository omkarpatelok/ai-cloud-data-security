package com.security.cloudscanner.policy;

public class PolicyDecisionResult {

    private String action;
    private String reason;

    public PolicyDecisionResult(String action, String reason) {
        this.action = action;
        this.reason = reason;
    }

    public String getAction() {
        return action;
    }

    public String getReason() {
        return reason;
    }
}