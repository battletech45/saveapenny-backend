package com.saveapenny.ocr.infrastructure.engine.tesseract;

import com.saveapenny.ocr.application.port.in.OcrService;
import com.saveapenny.ocr.domain.exception.OcrProcessingException;
import com.saveapenny.ocr.infrastructure.preprocessing.ImagePreprocessingService;
import com.saveapenny.config.OcrProperties;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import javax.imageio.ImageIO;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.core.task.TaskExecutor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class TesseractOcrService implements OcrService {

    private static final long ASYNC_FILE_SIZE_THRESHOLD_BYTES = 2L * 1024L * 1024L;

    private final OcrProperties ocrProperties;
    private final ImagePreprocessingService imagePreprocessingService;
    private final TaskExecutor ocrTaskExecutor;

    public TesseractOcrService(
            OcrProperties ocrProperties,
            ImagePreprocessingService imagePreprocessingService,
            @Qualifier("ocrTaskExecutor")
            TaskExecutor ocrTaskExecutor) {
        this.ocrProperties = ocrProperties;
        this.imagePreprocessingService = imagePreprocessingService;
        this.ocrTaskExecutor = ocrTaskExecutor;
    }

    @Override
    public String extractText(MultipartFile file) {
        validateInput(file);

        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if ("application/pdf".equals(contentType)) {
            return extractFromPdf(file);
        }

        try (InputStream inputStream = file.getInputStream()) {
            BufferedImage inputImage = ImageIO.read(inputStream);
            if (inputImage == null) {
                throw new OcrProcessingException("Unsupported file content");
            }

            BufferedImage preprocessedImage = imagePreprocessingService.preprocess(inputImage);
            Tesseract tesseract = createTesseract();
            return tesseract.doOCR(preprocessedImage).trim();
        } catch (IOException ex) {
            throw new OcrProcessingException("Unable to read uploaded file", ex);
        } catch (TesseractException ex) {
            throw new OcrProcessingException("OCR processing failed", ex);
        }
    }

    @Override
    public CompletableFuture<String> extractTextAsync(MultipartFile file) {
        if (file == null || file.getSize() < ASYNC_FILE_SIZE_THRESHOLD_BYTES) {
            return CompletableFuture.completedFuture(extractText(file));
        }
        return CompletableFuture.supplyAsync(() -> extractText(file), ocrTaskExecutor);
    }

    private void validateInput(MultipartFile file) {
        if (!ocrProperties.enabled()) {
            throw new OcrProcessingException("OCR is disabled");
        }
        if (file == null || file.isEmpty()) {
            throw new OcrProcessingException("Uploaded file is empty");
        }
    }

    private String extractFromPdf(MultipartFile file) {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFRenderer renderer = new PDFRenderer(document);
            Tesseract tesseract = createTesseract();
            StringBuilder builder = new StringBuilder();
            for (int page = 0; page < document.getNumberOfPages(); page++) {
                BufferedImage pageImage = renderer.renderImageWithDPI(page, 300);
                BufferedImage preprocessed = imagePreprocessingService.preprocess(pageImage);
                String pageText = tesseract.doOCR(preprocessed).trim();
                if (!pageText.isBlank()) {
                    if (!builder.isEmpty()) {
                        builder.append(System.lineSeparator());
                    }
                    builder.append(pageText);
                }
            }
            return builder.toString();
        } catch (IOException ex) {
            throw new OcrProcessingException("Unable to read uploaded PDF", ex);
        } catch (TesseractException ex) {
            throw new OcrProcessingException("OCR processing failed", ex);
        }
    }

    private Tesseract createTesseract() {
        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath(ocrProperties.tessdataPath());
        tesseract.setLanguage(ocrProperties.language());
        tesseract.setPageSegMode(ocrProperties.psm());
        return tesseract;
    }
}
