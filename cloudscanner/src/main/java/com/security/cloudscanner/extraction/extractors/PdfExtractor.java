package com.security.cloudscanner.extraction.extractors;

import com.security.cloudscanner.extraction.ExtractionResult;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class PdfExtractor {

    public ExtractionResult extract(byte[] contentBytes, String fileType) {
        try (PDDocument document = PDDocument.load(contentBytes)) {
            if (document.isEncrypted()) {
                return ExtractionResult.encrypted(fileType);
            }

            String text = new PDFTextStripper().getText(document);
            if (text == null || text.isBlank()) {
                return ExtractionResult.ocrRequired(fileType);
            }
            return ExtractionResult.scanned(text, fileType);
        } catch (InvalidPasswordException e) {
            return ExtractionResult.encrypted(fileType);
        } catch (IOException e) {
            return ExtractionResult.failed(fileType);
        }
    }
}
