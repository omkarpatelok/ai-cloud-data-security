package com.security.cloudscanner.extraction;

import org.apache.tika.Tika;
import org.springframework.stereotype.Component;

@Component
public class FileTypeDetector {

    private final Tika tika = new Tika();

    public String detect(byte[] contentBytes, String resourceName) {
        if (contentBytes == null || contentBytes.length == 0) {
            return "application/octet-stream";
        }

        try {
            return tika.detect(contentBytes, resourceName);
        } catch (Exception e) {
            return "application/octet-stream";
        }
    }
}
