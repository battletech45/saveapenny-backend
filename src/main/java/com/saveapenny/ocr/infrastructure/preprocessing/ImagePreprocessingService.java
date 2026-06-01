package com.saveapenny.ocr.infrastructure.preprocessing;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import org.springframework.stereotype.Component;

@Component
public class ImagePreprocessingService {

    private static final int MIN_WIDTH = 1600;

    public BufferedImage preprocess(BufferedImage original) {
        BufferedImage normalized = upscaleIfNeeded(original);
        BufferedImage grayscale = toGrayscale(normalized);
        return applyThreshold(grayscale, 140);
    }

    private BufferedImage upscaleIfNeeded(BufferedImage input) {
        if (input.getWidth() >= MIN_WIDTH) {
            return input;
        }

        double scale = (double) MIN_WIDTH / input.getWidth();
        int targetWidth = MIN_WIDTH;
        int targetHeight = Math.max(1, (int) Math.round(input.getHeight() * scale));

        BufferedImage resized = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = resized.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.drawImage(input, 0, 0, targetWidth, targetHeight, null);
        } finally {
            graphics.dispose();
        }
        return resized;
    }

    private BufferedImage toGrayscale(BufferedImage input) {
        BufferedImage grayscale = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D graphics = grayscale.createGraphics();
        try {
            graphics.drawImage(input, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return grayscale;
    }

    private BufferedImage applyThreshold(BufferedImage input, int threshold) {
        BufferedImage output = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
        for (int y = 0; y < input.getHeight(); y++) {
            for (int x = 0; x < input.getWidth(); x++) {
                int pixel = input.getRGB(x, y) & 0xFF;
                int binary = pixel < threshold ? 0x00 : 0xFF;
                int rgb = (binary << 16) | (binary << 8) | binary;
                output.setRGB(x, y, rgb);
            }
        }
        return output;
    }
}
