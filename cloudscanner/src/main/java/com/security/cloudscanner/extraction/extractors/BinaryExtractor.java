package com.security.cloudscanner.extraction.extractors;

import com.security.cloudscanner.extraction.ExtractionResult;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class BinaryExtractor {

    public ExtractionResult extract(byte[] contentBytes, String fileType) {
        if (contentBytes == null || contentBytes.length == 0) {
            return ExtractionResult.unsupported(fileType);
        }

        StringBuilder builder = new StringBuilder();
        StringBuilder current = new StringBuilder();

        for (byte value : contentBytes) {
            int unsigned = value & 0xFF;
            if (unsigned >= 32 && unsigned <= 126) {
                current.append((char) unsigned);
            } else {
                appendCandidate(builder, current);
            }
        }
        appendCandidate(builder, current);

        if (builder.length() == 0) {
            return ExtractionResult.unsupported(fileType);
        }

        String extracted = builder.toString().trim();
        if (extracted.isBlank()) {
            return ExtractionResult.unsupported(fileType);
        }

        return ExtractionResult.scanned(new String(extracted.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8), fileType);
    }

    private void appendCandidate(StringBuilder builder, StringBuilder current) {
        if (current.length() >= 4) {
            builder.append(current).append(System.lineSeparator());
        }
        current.setLength(0);
    }
}
