package com.saveapenny.ocr.application.port.in;

public record OcrUploadPayload(
        String originalFileName,
        String contentType,
        byte[] content) {

    public boolean isEmpty() {
        return content == null || content.length == 0;
    }

    public long size() {
        return content == null ? 0L : content.length;
    }
}
