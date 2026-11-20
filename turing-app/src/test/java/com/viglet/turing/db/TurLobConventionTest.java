package com.viglet.turing.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * T1320 — no entity field is mapped with {@code @Lob}. On a String it binds as a
 * CLOB, which PostgreSQL may handle as a large object while Liquibase created the
 * column as {@code text}; the column type then depends on the dialect. Large text
 * is {@code @Column(columnDefinition = "longtext")}, the portable type the
 * persistence rules use in changelogs.
 */
class TurLobConventionTest {

    private static final Pattern LOB = Pattern.compile(
            "(?m)^\\s*@(jakarta\\.persistence\\.)?Lob\\b|^import jakarta\\.persistence\\.Lob;");

    @Test
    void noEntityFieldUsesLob() throws IOException {
        Path sources = Paths.get("src", "main", "java");
        Set<String> offenders = new TreeSet<>();
        try (Stream<Path> paths = Files.walk(sources)) {
            paths.filter(p -> p.toString().endsWith(".java")).forEach(p -> {
                try {
                    if (LOB.matcher(Files.readString(p)).find()) {
                        offenders.add(sources.relativize(p).toString());
                    }
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            });
        }
        assertThat(offenders)
                .as("map large text as @Column(columnDefinition = \"longtext\"), not @Lob (T1320)")
                .isEmpty();
    }
}
