package com.security.cloudscanner.extraction.extractors;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class EncryptedFileHandler {

    public boolean isEncrypted(String fileType, byte[] contentBytes) {
        if (contentBytes == null || contentBytes.length == 0) {
            return false;
        }

        if (fileType != null && fileType.toLowerCase().contains("encrypted")) {
            return true;
        }

        String header = new String(contentBytes, 0, Math.min(contentBytes.length, 2048), StandardCharsets.ISO_8859_1);
        if ("application/pdf".equals(fileType) && header.contains("/Encrypt")) {
            return true;
        }

        return header.contains("EncryptedPackage");
    }
}
