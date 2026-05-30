package com.security.cloudscanner.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class RegexDetectionService {

    private static final Map<String, Pattern> DETECTION_PATTERNS = buildPatterns();

    private static Map<String, Pattern> buildPatterns() {
        Map<String, Pattern> patterns = new LinkedHashMap<>();
        patterns.put("AADHAAR", Pattern.compile("\\b\\d{4}[ -]?\\d{4}[ -]?\\d{4}\\b"));
        patterns.put("PAN", Pattern.compile("\\b[A-Z]{5}[0-9]{4}[A-Z]\\b"));
        patterns.put("CREDIT_CARD", Pattern.compile("\\b(?:\\d[ -]*?){13,16}\\b"));
        patterns.put("API_KEY", Pattern.compile("(?i)api[_-]?key\\s*[:=]\\s*[\"']?[A-Za-z0-9_\\-]{16,}[\"']?"));
        patterns.put("AWS_ACCESS_KEY", Pattern.compile("\\bAKIA[0-9A-Z]{16}\\b"));
        patterns.put("JWT_SECRET", Pattern.compile("(?i)jwt[_-]?secret\\s*[:=]\\s*[\"']?.{8,}[\"']?"));
        patterns.put("PRIVATE_KEY", Pattern.compile("-----BEGIN(?: RSA)? PRIVATE KEY-----"));
        patterns.put("DB_CREDENTIAL", Pattern.compile("(?i)(password|passwd|db_password|database_password)\\s*[:=]\\s*[\"']?.{6,}[\"']?"));
        return patterns;
    }

    public List<String> detect(String content) {
        List<String> findings = new ArrayList<>();

        if (content == null || content.isBlank()) {
            return findings;
        }

        for (Map.Entry<String, Pattern> entry : DETECTION_PATTERNS.entrySet()) {
            if (entry.getValue().matcher(content).find()) {
                findings.add(entry.getKey());
            }
        }

        return findings;
    }
}
