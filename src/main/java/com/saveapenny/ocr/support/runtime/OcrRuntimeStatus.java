package com.saveapenny.ocr.support.runtime;

public record OcrRuntimeStatus(
        boolean enabled,
        boolean tessdataPathValid,
        boolean nativeLibraryLoaded,
        String language,
        String tessdataPath,
        String message) {

    public boolean ready() {
        return !enabled || (tessdataPathValid && nativeLibraryLoaded);
    }
}
