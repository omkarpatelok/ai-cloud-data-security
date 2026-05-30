package com.security.cloudscanner.extraction;

import com.security.cloudscanner.extraction.extractors.BinaryExtractor;
import com.security.cloudscanner.extraction.extractors.EncryptedFileHandler;
import com.security.cloudscanner.extraction.extractors.ImageOcrExtractor;
import com.security.cloudscanner.extraction.extractors.OfficeExtractor;
import com.security.cloudscanner.extraction.extractors.PdfExtractor;
import com.security.cloudscanner.extraction.extractors.PlainTextExtractor;
import org.springframework.stereotype.Service;

@Service
public class ContentExtractionService {

    private final FileTypeDetector fileTypeDetector;
    private final PlainTextExtractor plainTextExtractor;
    private final PdfExtractor pdfExtractor;
    private final OfficeExtractor officeExtractor;
    private final ImageOcrExtractor imageOcrExtractor;
    private final BinaryExtractor binaryExtractor;
    private final EncryptedFileHandler encryptedFileHandler;

    public ContentExtractionService(
            FileTypeDetector fileTypeDetector,
            PlainTextExtractor plainTextExtractor,
            PdfExtractor pdfExtractor,
            OfficeExtractor officeExtractor,
            ImageOcrExtractor imageOcrExtractor,
            BinaryExtractor binaryExtractor,
            EncryptedFileHandler encryptedFileHandler
    ) {
        this.fileTypeDetector = fileTypeDetector;
        this.plainTextExtractor = plainTextExtractor;
        this.pdfExtractor = pdfExtractor;
        this.officeExtractor = officeExtractor;
        this.imageOcrExtractor = imageOcrExtractor;
        this.binaryExtractor = binaryExtractor;
        this.encryptedFileHandler = encryptedFileHandler;
    }

    public ExtractionResult extract(String resourceName, byte[] contentBytes) {
        try {
            String fileType = fileTypeDetector.detect(contentBytes, resourceName);

            if (encryptedFileHandler.isEncrypted(fileType, contentBytes)) {
                return ExtractionResult.encrypted(fileType);
            }

            if (isPlainText(fileType)) {
                return plainTextExtractor.extract(contentBytes, fileType);
            }

            if ("application/pdf".equals(fileType)) {
                return pdfExtractor.extract(contentBytes, fileType);
            }

            if (isOfficeDocument(fileType)) {
                return officeExtractor.extract(contentBytes, fileType);
            }

            if (fileType.startsWith("image/")) {
                return imageOcrExtractor.extract(contentBytes, fileType);
            }

            return binaryExtractor.extract(contentBytes, fileType);
        } catch (Exception e) {
            System.out.println("Extraction routing failed for " + resourceName + ": " + e.getMessage());
            return ExtractionResult.failed("application/octet-stream");
        }
    }

    private boolean isPlainText(String fileType) {
        if (fileType == null) {
            return false;
        }

        return fileType.startsWith("text/")
                || "application/json".equals(fileType)
                || "application/xml".equals(fileType)
                || "application/x-yaml".equals(fileType)
                || "application/x-java-properties".equals(fileType)
                || "application/javascript".equals(fileType)
                || "application/sql".equals(fileType);
    }

    private boolean isOfficeDocument(String fileType) {
        if (fileType == null) {
            return false;
        }

        return "application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(fileType)
                || "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet".equals(fileType)
                || "application/vnd.openxmlformats-officedocument.presentationml.presentation".equals(fileType);
    }
}
