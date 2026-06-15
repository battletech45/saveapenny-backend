package com.saveapenny.ocr.support.runtime;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.sourceforge.tess4j.TessAPI;

public final class OcrRuntimeEnvironment {

    private OcrRuntimeEnvironment() {
    }

    public static String resolveTessdataPath(String configuredPath) {
        String explicitPath = normalizeConfiguredTessdataPath(configuredPath);
        if (explicitPath != null) {
            return explicitPath;
        }

        String envPath = normalizeDetectedTessdataPath(System.getenv("TESSDATA_PREFIX"));
        if (envPath != null) {
            return envPath;
        }

        for (Path candidate : commonTessdataDirectories()) {
            if (Files.isDirectory(candidate)) {
                return candidate.toString();
            }
        }

        return null;
    }

    public static boolean isTessdataAvailable(String tessdataPath, String language) {
        if (tessdataPath == null || tessdataPath.isBlank()) {
            return false;
        }

        Path directory = Path.of(tessdataPath.trim());
        if (!Files.isDirectory(directory)) {
            return false;
        }

        List<String> languages = languages(language);
        if (languages.isEmpty()) {
            return true;
        }

        for (String entry : languages) {
            if (!Files.isRegularFile(directory.resolve(entry + ".traineddata"))) {
                return false;
            }
        }
        return true;
    }

    public static void configureNativeLibraryPathIfMissing() {
        if (hasText(System.getProperty("jna.library.path"))) {
            return;
        }

        String detected = detectNativeLibraryPath();
        if (detected != null) {
            System.setProperty("jna.library.path", detected);
        }
    }

    public static String detectNativeLibraryPath() {
        String configured = System.getProperty("java.library.path");
        if (hasText(configured)) {
            for (String value : configured.split(System.getProperty("path.separator"))) {
                if (containsNativeLibrary(Path.of(value))) {
                    return value;
                }
            }
        }

        for (Path candidate : commonNativeLibraryDirectories()) {
            if (containsNativeLibrary(candidate)) {
                return candidate.toString();
            }
        }

        return null;
    }

    public static boolean canLoadNativeTesseract() {
        try {
            configureNativeLibraryPathIfMissing();
            String version = TessAPI.INSTANCE.TessVersion();
            return version != null && !version.isBlank();
        } catch (Throwable ex) {
            return false;
        }
    }

    private static String normalizeConfiguredTessdataPath(String configuredPath) {
        if (!hasText(configuredPath)) {
            return null;
        }

        Path candidate = Path.of(configuredPath.trim());
        if (Files.isDirectory(candidate.resolve("tessdata"))) {
            return candidate.resolve("tessdata").toString();
        }
        return candidate.toString();
    }

    private static String normalizeDetectedTessdataPath(String configuredPath) {
        String normalized = normalizeConfiguredTessdataPath(configuredPath);
        if (normalized == null) {
            return null;
        }
        return Files.isDirectory(Path.of(normalized)) ? normalized : null;
    }

    private static List<String> languages(String language) {
        if (!hasText(language)) {
            return List.of();
        }

        List<String> entries = new ArrayList<>();
        for (String value : language.split("\\+")) {
            if (hasText(value)) {
                entries.add(value.trim());
            }
        }
        return entries;
    }

    private static List<Path> commonTessdataDirectories() {
        List<Path> directories = new ArrayList<>(List.of(
                Path.of("/opt/homebrew/share/tessdata"),
                Path.of("/usr/local/share/tessdata"),
                Path.of("/usr/share/tesseract-ocr/5/tessdata"),
                Path.of("/usr/share/tesseract-ocr/4.00/tessdata"),
                Path.of("/usr/share/tessdata"),
                Path.of("/opt/local/share/tessdata")
        ));

        String programFiles = System.getenv("ProgramFiles");
        if (hasText(programFiles)) {
            directories.add(Path.of(programFiles, "Tesseract-OCR", "tessdata"));
        }
        return directories;
    }

    private static List<Path> commonNativeLibraryDirectories() {
        List<Path> directories = new ArrayList<>(List.of(
                Path.of("/opt/homebrew/lib"),
                Path.of("/usr/local/lib"),
                Path.of("/usr/lib/x86_64-linux-gnu"),
                Path.of("/usr/lib/aarch64-linux-gnu"),
                Path.of("/usr/lib64"),
                Path.of("/usr/lib"),
                Path.of("/opt/local/lib")
        ));

        String programFiles = System.getenv("ProgramFiles");
        if (hasText(programFiles)) {
            directories.add(Path.of(programFiles, "Tesseract-OCR"));
        }
        return directories;
    }

    private static boolean containsNativeLibrary(Path directory) {
        if (!Files.isDirectory(directory)) {
            return false;
        }

        try (var entries = Files.list(directory)) {
            return entries
                    .map(path -> path.getFileName().toString().toLowerCase(Locale.ROOT))
                    .anyMatch(name -> name.startsWith("libtesseract") || (name.startsWith("tesseract") && name.endsWith(".dll")));
        } catch (Exception ex) {
            return false;
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
