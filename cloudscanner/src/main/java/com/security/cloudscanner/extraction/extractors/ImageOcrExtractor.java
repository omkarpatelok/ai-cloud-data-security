package com.security.cloudscanner.extraction.extractors;

import com.security.cloudscanner.extraction.ExtractionResult;
import net.sourceforge.tess4j.Tesseract;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class ImageOcrExtractor {

    private static final long OCR_MAX_FILE_SIZE = 5_000_000L;
    private static final String TESSDATA_PATH = "C:\\Program Files\\Tesseract-OCR\\tessdata";

    public ExtractionResult extract(byte[] contentBytes, String fileType) {
        if (contentBytes == null || contentBytes.length == 0 || contentBytes.length > OCR_MAX_FILE_SIZE) {
            return ExtractionResult.ocrRequired(fileType);
        }

        try {
            Path tessdata = Path.of(TESSDATA_PATH);
            if (!Files.exists(tessdata)) {
                return ExtractionResult.ocrRequired(fileType);
            }

            BufferedImage image = ImageIO.read(new ByteArrayInputStream(contentBytes));
            if (image == null) {
                return ExtractionResult.ocrRequired(fileType);
            }

            Tesseract tesseract = new Tesseract();
            tesseract.setDatapath(TESSDATA_PATH);
            String text = tesseract.doOCR(image);
            return ExtractionResult.scanned(text, fileType);
        } catch (Throwable t) {
            return ExtractionResult.ocrRequired(fileType);
        }
    }
}
