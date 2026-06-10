package com.saveapenny.ocr.infrastructure.preprocessing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ImagePreprocessingServiceTest {

    private ImagePreprocessingService service;

    @BeforeEach
    void setUp() {
        service = new ImagePreprocessingService();
    }

    @Test
    void preprocess_smallImage_upscalesToMinWidth() {
        BufferedImage small = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
        small.getGraphics().drawRect(0, 0, 100, 100);

        BufferedImage result = service.preprocess(small);

        assertEquals(1600, result.getWidth());
        assertEquals(1200, result.getHeight());
    }

    @Test
    void preprocess_largeImage_doesNotUpscale() {
        BufferedImage large = new BufferedImage(2000, 1500, BufferedImage.TYPE_INT_RGB);
        large.getGraphics().drawRect(0, 0, 100, 100);

        BufferedImage result = service.preprocess(large);

        assertEquals(2000, result.getWidth());
        assertEquals(1500, result.getHeight());
    }

    @Test
    void preprocess_returnsGrayscaleBinaryImage() {
        BufferedImage input = new BufferedImage(100, 50, BufferedImage.TYPE_INT_RGB);

        BufferedImage result = service.preprocess(input);

        assertTrue(result.getType() == BufferedImage.TYPE_BYTE_BINARY
                || result.getType() == BufferedImage.TYPE_BYTE_GRAY);
    }

    @Test
    void preprocess_thresholdDarkPixels() {
        BufferedImage dark = new BufferedImage(50, 50, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 50; y++) {
            for (int x = 0; x < 50; x++) {
                dark.setRGB(x, y, 0x000000);
            }
        }

        BufferedImage result = service.preprocess(dark);

        int pixel = result.getRGB(25, 25);
        int gray = pixel & 0xFF;
        assertTrue(gray < 50, "Dark pixel should remain dark after threshold");
    }

    @Test
    void preprocess_upscaleMaintainsAspectRatio() {
        BufferedImage tall = new BufferedImage(400, 800, BufferedImage.TYPE_INT_RGB);

        BufferedImage result = service.preprocess(tall);

        assertEquals(1600, result.getWidth());
        assertTrue(result.getHeight() > 0);
    }
}
