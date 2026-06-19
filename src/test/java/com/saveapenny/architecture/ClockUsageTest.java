package com.saveapenny.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ClockUsageTest {

    private static final Path SRC_DIR = Paths.get("src/main/java");
    private static final List<String> ZERO_ARG_NOW_CALLS = List.of(
            "Instant.now()",
            "LocalDate.now()",
            "LocalDateTime.now()",
            "ZonedDateTime.now()"
    );

    @Test
    void noDirectZeroArgNowCalls() throws IOException {
        try (Stream<Path> files = Files.walk(SRC_DIR)) {
            List<String> violations = files
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .flatMap(file -> {
                        try {
                            List<String> lines = Files.readAllLines(file);
                            return lines.stream()
                                    .filter(line -> ZERO_ARG_NOW_CALLS.stream().anyMatch(line::contains))
                                    .map(line -> {
                                        int lineNum = lines.indexOf(line) + 1;
                                        return SRC_DIR.relativize(file) + ":" + lineNum + " -> " + line.trim();
                                    });
                        } catch (IOException e) {
                            return Stream.empty();
                        }
                    })
                    .toList();

            assertTrue(violations.isEmpty(),
                    "Direct zero-arg now() calls found (use TimeService instead):\n  " + String.join("\n  ", violations));
        }
    }
}
