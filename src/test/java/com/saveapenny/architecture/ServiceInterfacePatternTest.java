package com.saveapenny.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceInterfacePatternTest {

    private static final Path SRC_DIR = Paths.get("src/main/java");
    private static final List<String> IGNORED_PATHS = List.of(
            "ocr/application/port/in/",
            "ocr/application/analysis/",
            "ocr/application/extractor/",
            "ocr/infrastructure/preprocessing/",
            "ocr/infrastructure/engine/tesseract/",
            "insight/service/impl/",
            "imports/service/impl/"
    );

    @Test
    void eachServiceInterfaceHasMatchingImplementation() throws IOException {
        List<String> missingImpls = new ArrayList<>();

        try (Stream<Path> files = Files.walk(SRC_DIR)) {
            List<Path> serviceFiles = files
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith("Service.java"))
                    .filter(p -> !p.toString().endsWith("ServiceImpl.java"))
                    .toList();

            for (Path file : serviceFiles) {
                String relative = SRC_DIR.relativize(file).toString();

                boolean ignored = IGNORED_PATHS.stream().anyMatch(relative::contains);
                if (ignored) {
                    continue;
                }

                String content = Files.readString(file);
                boolean isInterface = content.contains("interface ");

                if (!isInterface) {
                    continue;
                }

                String implPath = relative.replace("Service.java", "ServiceImpl.java")
                        .replace("/service/", "/service/impl/");

                Path fullImplPath = SRC_DIR.resolve(implPath);
                if (!Files.exists(fullImplPath)) {
                    missingImpls.add(relative + " (expected impl at " + implPath + ")");
                }
            }
        }

        assertTrue(missingImpls.isEmpty(),
                "Service interfaces without matching implementations:\n  " + String.join("\n  ", missingImpls));
    }
}
