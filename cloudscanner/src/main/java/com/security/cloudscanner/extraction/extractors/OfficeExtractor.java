package com.security.cloudscanner.extraction.extractors;

import com.security.cloudscanner.extraction.ExtractionResult;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.StringJoiner;

@Component
public class OfficeExtractor {

    public ExtractionResult extract(byte[] contentBytes, String fileType) {
        try {
            return switch (fileType) {
                case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" ->
                        extractDocx(contentBytes, fileType);
                case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" ->
                        extractXlsx(contentBytes, fileType);
                case "application/vnd.openxmlformats-officedocument.presentationml.presentation" ->
                        extractPptx(contentBytes, fileType);
                default -> ExtractionResult.unsupported(fileType);
            };
        } catch (Exception e) {
            return ExtractionResult.failed(fileType);
        }
    }

    private ExtractionResult extractDocx(byte[] contentBytes, String fileType) throws IOException {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(contentBytes))) {
            StringJoiner joiner = new StringJoiner(System.lineSeparator());
            document.getParagraphs().forEach(paragraph -> joiner.add(paragraph.getText()));
            return ExtractionResult.scanned(joiner.toString(), fileType);
        }
    }

    private ExtractionResult extractXlsx(byte[] contentBytes, String fileType) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(contentBytes))) {
            DataFormatter formatter = new DataFormatter();
            StringJoiner joiner = new StringJoiner(System.lineSeparator());
            workbook.forEach(sheet -> sheet.forEach(row -> row.forEach(cell -> joiner.add(formatter.formatCellValue(cell)))));
            return ExtractionResult.scanned(joiner.toString(), fileType);
        }
    }

    private ExtractionResult extractPptx(byte[] contentBytes, String fileType) throws IOException {
        try (XMLSlideShow slideShow = new XMLSlideShow(new ByteArrayInputStream(contentBytes))) {
            StringJoiner joiner = new StringJoiner(System.lineSeparator());
            slideShow.getSlides().forEach(slide -> slide.getShapes().forEach(shape -> appendSlideText(joiner, shape)));
            return ExtractionResult.scanned(joiner.toString(), fileType);
        }
    }

    private void appendSlideText(StringJoiner joiner, XSLFShape shape) {
        if (shape instanceof XSLFTextShape textShape) {
            joiner.add(textShape.getText());
        }
    }
}
