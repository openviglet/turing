package com.viglet.turing.modulith;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;

import jakarta.persistence.EntityListeners;

/**
 * T1316 — an entity names only listeners of its own module (or the shared kernel
 * and framework). A module that caches another's data listens to that module's
 * events; it is never named by the other's entity, which was the back-edge
 * {@code TurAIAgent} → sn snapshot listener. Read from bytecode, so the annotation
 * cannot be spelled around.
 */
class TurEntityListenerModuleTest {

    private static final String ROOT = "com.viglet.turing.";

    @Test
    void noEntityNamesAnotherModulesListener() throws IOException {
        Path classes = Paths.get("target/classes/com/viglet/turing");
        assertThat(Files.isDirectory(classes)).as("compiled classes at %s", classes).isTrue();
        SimpleMetadataReaderFactory readers = new SimpleMetadataReaderFactory();
        Set<String> crossings = new TreeSet<>();
        Set<String> seen = new TreeSet<>();
        try (Stream<Path> paths = Files.walk(classes)) {
            paths.filter(p -> p.toString().endsWith(".class")).forEach(p -> {
                try {
                    MetadataReader reader = readers.getMetadataReader(new FileSystemResource(p));
                    var attributes = reader.getAnnotationMetadata()
                            .getAnnotationAttributes(EntityListeners.class.getName(), true);
                    if (attributes == null || !(attributes.get("value") instanceof String[] listeners)) {
                        return;
                    }
                    String entity = reader.getClassMetadata().getClassName();
                    seen.add(entity.substring(entity.lastIndexOf('.') + 1));
                    for (String listener : listeners) {
                        String listenerModule = module(listener);
                        if (listenerModule != null && !listenerModule.equals("shared")
                                && !listenerModule.equals(module(entity))) {
                            crossings.add(entity + " -> " + listener);
                        }
                    }
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            });
        }
        assertThat(seen).as("the scan reads the listener-bearing entities")
                .contains("TurAIAgent", "TurSNSite", "TurPersona", "TurChatFlow");
        assertThat(crossings).as("entities naming another module's listener").isEmpty();
    }

    /** The first package segment under com.viglet.turing, or null for a framework class. */
    private static String module(String className) {
        if (!className.startsWith(ROOT)) {
            return null;
        }
        String rest = className.substring(ROOT.length());
        int dot = rest.indexOf('.');
        return dot < 0 ? "" : rest.substring(0, dot);
    }
}