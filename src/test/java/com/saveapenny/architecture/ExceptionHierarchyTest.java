package com.saveapenny.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ExceptionHierarchyTest {

    private static final Path SRC_DIR = Paths.get("src/main/java");

    @Test

    void allExceptionsExtendRuntimeException() throws IOException {
        List<String> violations = new ArrayList<>();

        try (Stream<Path> files = Files.walk(SRC_DIR)) {
            List<Path> exceptionFiles = files
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith("Exception.java"))
                    .toList();

            var extendsRuntime = new java.util.HashSet<String>();
            for (Path file : exceptionFiles) {
                String content = Files.readString(file);
                if (content.contains("extends RuntimeException")) {
                    extendsRuntime.add(SRC_DIR.relativize(file).toString()
                            .replace('/', '.').replace(".java", ""));
                }
            }

            for (Path file : exceptionFiles) {
                String content = Files.readString(file);
                String relative = SRC_DIR.relativize(file).toString();

                if (content.contains("extends RuntimeException")) {
                    continue;
                }

                java.util.regex.Matcher m = java.util.regex.Pattern.compile("extends\\s+(\\w+)Exception").matcher(content);
                boolean hasValidParent = false;
                while (m.find()) {
                    String parent = m.group(1) + "Exception";
                    for (String known : extendsRuntime) {
                        if (known.endsWith("." + parent)) {
                            hasValidParent = true;
                            break;
                        }
                    }
                }

                if (!hasValidParent) {
                    violations.add(relative);
                }
            }
        }

        assertTrue(violations.isEmpty(),
                "Exception classes that do not extend RuntimeException (directly or transitively):\n  " + String.join("\n  ", violations));
    }

    @Test

    void exceptionsInCorrectPackage() throws IOException {
        List<String> violations = new ArrayList<>();

        try (Stream<Path> files = Files.walk(SRC_DIR)) {
            List<Path> exceptionFiles = files
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith("Exception.java"))
                    .toList();

            for (Path file : exceptionFiles) {
                Path relative = SRC_DIR.relativize(file);
                String path = relative.toString();

                boolean inExceptionPackage = path.contains("/exception/");
                boolean inErrorPackage = path.contains("/error/");

                if (!inExceptionPackage && !inErrorPackage) {
                    violations.add(path);
                }
            }
        }

        assertTrue(violations.isEmpty(),
                "Exception classes outside exception/ or error/ packages:\n  " + String.join("\n  ", violations));
    }
}
