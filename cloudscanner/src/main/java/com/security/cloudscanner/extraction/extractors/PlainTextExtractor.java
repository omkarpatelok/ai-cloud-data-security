package com.security.cloudscanner.extraction.extractors;

import com.security.cloudscanner.extraction.ExtractionResult;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class PlainTextExtractor {

    private static final List<Charset> CANDIDATE_CHARSETS = List.of(
            StandardCharsets.UTF_8,
            StandardCharsets.UTF_16,
            StandardCharsets.ISO_8859_1,
            Charset.forName("windows-1252")
    );

    public ExtractionResult extract(byte[] contentBytes, String fileType) {
        if (contentBytes == null) {
            return ExtractionResult.failed(fileType);
        }

        for (Charset charset : CANDIDATE_CHARSETS) {
            try {
                CharsetDecoder decoder = charset.newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT);
                CharBuffer decoded = decoder.decode(ByteBuffer.wrap(contentBytes));
                return ExtractionResult.scanned(decoded.toString(), fileType);
            } catch (CharacterCodingException ignored) {
                // try next charset
            }
        }

        return ExtractionResult.failed(fileType);
    }
}
