package com.saveapenny.ocr.infrastructure.engine.tesseract;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.saveapenny.config.OcrProperties;
import com.saveapenny.ocr.infrastructure.engine.tesseract.TesseractOcrService;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
class OcrGoldenImageRegressionTest {

    private static final String TESSERACT_PREFIX = resolveTesseractPrefix();
    private static final Path TESSDATA_PATH = TESSERACT_PREFIX == null ? null : Path.of(TESSERACT_PREFIX, "share", "tessdata");
    private static final Path TESSERACT_LIB = TESSERACT_PREFIX == null ? null : Path.of(TESSERACT_PREFIX, "lib", "libtesseract.dylib");

    @BeforeAll
    static void preloadNativeLibraryPath() {
        Assumptions.assumeTrue(TESSERACT_LIB != null);
        Assumptions.assumeTrue(Files.isRegularFile(TESSERACT_LIB));
        System.setProperty("jna.library.path", TESSERACT_LIB.getParent().toString());
    }

    @DynamicPropertySource
    static void configureOcrProperties(DynamicPropertyRegistry registry) {
        if (TESSDATA_PATH != null) {
            registry.add("ocr.tessdata-path", () -> TESSDATA_PATH.toString());
        }
    }

    @Autowired
    private TesseractOcrService tesseractOcrService;

    @Autowired
    private OcrProperties ocrProperties;

    @Test
    void goldenImage_extractsExpectedKeyword() throws Exception {
        Assumptions.assumeTrue(ocrProperties.enabled());
        Assumptions.assumeTrue(Files.isDirectory(Path.of(ocrProperties.tessdataPath())));

        BufferedImage image = new BufferedImage(1200, 350, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        graphics.setColor(Color.BLACK);
        graphics.setFont(new Font("Serif", Font.BOLD, 72));
        graphics.drawString("SAVEAPENNY RECEIPT", 80, 180);
        graphics.drawString("TOTAL 19.99", 80, 280);
        graphics.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        MockMultipartFile file = new MockMultipartFile("file", "golden.png", "image/png", baos.toByteArray());

        String text = tesseractOcrService.extractText(file).toUpperCase();
        assertTrue(text.contains("SAVEAPENNY") || text.contains("RECEIPT"));
        assertTrue(text.contains("19.99") || text.contains("TOTAL"));
    }

    private static String resolveTesseractPrefix() {
        try {
            Process process = new ProcessBuilder("brew", "--prefix", "tesseract").start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                return null;
            }
            List<String> lines = new String(process.getInputStream().readAllBytes()).lines().toList();
            if (lines.isEmpty() || lines.getFirst().isBlank()) {
                return null;
            }
            return lines.getFirst().trim();
        } catch (Exception ex) {
            return null;
        }
    }
}
