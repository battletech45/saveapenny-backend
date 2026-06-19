package com.saveapenny.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NoHardcodedSecretsTest {

    private static final Path CONFIG_DIR = Paths.get("src/main/resources");
    private static final List<Pattern> SECRET_PATTERNS = List.of(
            Pattern.compile("password:\\s*[^$\\s]"),
            Pattern.compile("secret:\\s*[^$\\s]"),
            Pattern.compile("api-key:\\s*[^$\\s]"),
            Pattern.compile("api_key:\\s*[^$\\s]")
    );
    private static final List<Pattern> ALLOWED_PATTERNS = List.of(
            Pattern.compile("password:\\s*$"),
            Pattern.compile("secret:\\s*$"),
            Pattern.compile("api-key:\\s*$"),
            Pattern.compile("api_key:\\s*$")
    );

    @Test
    void noHardcodedSecretsInSharedConfig() throws IOException {
        try (Stream<Path> files = Files.walk(CONFIG_DIR)) {
            List<String> violations = files
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".yml") || p.toString().endsWith(".yaml") || p.toString().endsWith(".properties"))
                    .flatMap(file -> {
                        try {
                            List<String> lines = Files.readAllLines(file);
                            return lines.stream()
                                    .filter(line -> SECRET_PATTERNS.stream().anyMatch(p -> p.matcher(line).find()))
                                    .filter(line -> ALLOWED_PATTERNS.stream().noneMatch(p -> p.matcher(line).find()))
                                    .map(line -> file.relativize(CONFIG_DIR) + ":" + (lines.indexOf(line) + 1) + " -> " + line.trim());
                        } catch (IOException e) {
                            return Stream.empty();
                        }
                    })
                    .toList();

            assertTrue(violations.isEmpty(),
                    "Hardcoded secrets found in config files (use ${VAR} placeholders):\n  " + String.join("\n  ", violations));
        }
    }
}
