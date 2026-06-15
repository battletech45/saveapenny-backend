package com.saveapenny.ocr.infrastructure.engine.tesseract;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.saveapenny.config.OcrProperties;
import com.saveapenny.ocr.application.port.in.OcrUploadPayload;
import com.saveapenny.ocr.infrastructure.engine.tesseract.TesseractOcrService;
import com.saveapenny.ocr.support.runtime.OcrRuntimeEnvironment;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:ocr-golden;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "ocr.enabled=true",
        "security.jwt.secret=0123456789012345678901234567890123456789012345678901234567890123"
})
class OcrGoldenImageRegressionTest {

    private static final String TESSDATA_PATH = OcrRuntimeEnvironment.resolveTessdataPath(null);

    @BeforeAll
    static void preloadNativeLibraryPath() {
        OcrRuntimeEnvironment.configureNativeLibraryPathIfMissing();
        Assumptions.assumeTrue(OcrRuntimeEnvironment.canLoadNativeTesseract());
    }

    @DynamicPropertySource
    static void configureOcrProperties(DynamicPropertyRegistry registry) {
        if (TESSDATA_PATH != null) {
            registry.add("ocr.tessdata-path", () -> TESSDATA_PATH);
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
        OcrUploadPayload file = new OcrUploadPayload("golden.png", "image/png", baos.toByteArray());

        String text = tesseractOcrService.extractText(file).toUpperCase();
        assertTrue(text.contains("SAVEAPENNY") || text.contains("RECEIPT"));
        assertTrue(text.contains("19.99") || text.contains("TOTAL"));
    }
}
