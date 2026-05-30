package com.security.cloudscanner.service;

import org.springframework.stereotype.Service;

@Service
public class DataMaskingService {

    public String maskContent(String content) {

        if (content == null) return null;

        // Mask Aadhaar / Credit cards with spaces or hyphens
        content = content.replaceAll(
                "\\b\\d{4}[ -]?\\d{4}[ -]?\\d{4}[ -]?\\d{4}\\b",
                "XXXX-XXXX-XXXX-XXXX"
        );

        // Mask PAN
        content = content.replaceAll(
                "\\b[A-Z]{5}[0-9]{4}[A-Z]\\b",
                "XXXXX****X"
        );

        // Mask SSN / Phone-ish
        content = content.replaceAll(
                "\\b\\d{3}-\\d{3}-\\d{4}\\b",
                "XXX-XXX-XXXX"
        );

        // Mask email
        content = content.replaceAll(
                "[a-zA-Z0-9._%+-]+(@[a-zA-Z0-9.-]+)",
                "****$1"
        );

        // Mask AWS Keys
        content = content.replaceAll(
                "\\bAKIA[0-9A-Z]{16}\\b",
                "AKIA****************"
        );

        // Mask Secrets / Passwords
        content = content.replaceAll(
                "(?i)(password|secret|key)\\s*[:=]\\s*(.*)",
                "$1: ****************"
        );

        return content;
    }
}