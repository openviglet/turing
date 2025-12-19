/*
 * Copyright (C) 2016-2024 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If
 * not, see <https://www.gnu.org/licenses/>.
 */

package com.viglet.turing.commons.file;

import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for TurFileAttributes.
 *
 * @author Alexandre Oliveira
 * @since 0.3.9
 */
class TurFileAttributesTest {

    @Test
    void testDefaultConstructor() {
        TurFileAttributes attributes = new TurFileAttributes();

        assertThat(attributes.getContent()).isNull();
        assertThat(attributes.getName()).isNull();
        assertThat(attributes.getTitle()).isNull();
        assertThat(attributes.getExtension()).isNull();
        assertThat(attributes.getSize()).isNotNull();
        assertThat(attributes.getLastModified()).isNotNull();
        assertThat(attributes.getMetadata()).isNotNull().isEmpty();
    }

    @Test
    void testBuilderPattern() {
        Date testDate = new Date();
        TurFileSize testSize = new TurFileSize();
        Map<String, String> testMetadata = new HashMap<>();
        testMetadata.put("author", "Test Author");

        TurFileAttributes attributes = TurFileAttributes.builder()
                .content("Test content")
                .name("test.txt")
                .title("Test File")
                .extension("txt")
                .size(testSize)
                .lastModified(testDate)
                .metadata(testMetadata)
                .build();

        assertThat(attributes.getContent()).isEqualTo("Test content");
        assertThat(attributes.getName()).isEqualTo("test.txt");
        assertThat(attributes.getTitle()).isEqualTo("Test File");
        assertThat(attributes.getExtension()).isEqualTo("txt");
        assertThat(attributes.getSize()).isEqualTo(testSize);
        assertThat(attributes.getLastModified()).isEqualTo(testDate);
        assertThat(attributes.getMetadata()).containsEntry("author", "Test Author");
    }

    @Test
    void testAllArgsConstructor() {
        Date testDate = new Date();
        TurFileSize testSize = new TurFileSize();
        Map<String, String> testMetadata = new HashMap<>();
        testMetadata.put("type", "document");

        TurFileAttributes attributes = new TurFileAttributes(
                "Test content",
                "document.pdf",
                "Test Document",
                "pdf",
                testSize,
                testDate,
                testMetadata
        );

        assertThat(attributes.getContent()).isEqualTo("Test content");
        assertThat(attributes.getName()).isEqualTo("document.pdf");
        assertThat(attributes.getTitle()).isEqualTo("Test Document");
        assertThat(attributes.getExtension()).isEqualTo("pdf");
        assertThat(attributes.getSize()).isEqualTo(testSize);
        assertThat(attributes.getLastModified()).isEqualTo(testDate);
        assertThat(attributes.getMetadata()).containsEntry("type", "document");
    }

    @Test
    void testSettersAndGetters() {
        TurFileAttributes attributes = new TurFileAttributes();

        attributes.setContent("Modified content");
        attributes.setName("modified.txt");
        attributes.setTitle("Modified Title");
        attributes.setExtension("txt");

        Date newDate = new Date(System.currentTimeMillis() + 1000);
        attributes.setLastModified(newDate);

        TurFileSize newSize = new TurFileSize();
        attributes.setSize(newSize);

        Map<String, String> newMetadata = new HashMap<>();
        newMetadata.put("updated", "true");
        attributes.setMetadata(newMetadata);

        assertThat(attributes.getContent()).isEqualTo("Modified content");
        assertThat(attributes.getName()).isEqualTo("modified.txt");
        assertThat(attributes.getTitle()).isEqualTo("Modified Title");
        assertThat(attributes.getExtension()).isEqualTo("txt");
        assertThat(attributes.getLastModified()).isEqualTo(newDate);
        assertThat(attributes.getSize()).isEqualTo(newSize);
        assertThat(attributes.getMetadata()).containsEntry("updated", "true");
    }

    @Test
    void testToBuilder() {
        TurFileAttributes original = TurFileAttributes.builder()
                .content("Original content")
                .name("original.txt")
                .title("Original Title")
                .extension("txt")
                .build();

        TurFileAttributes modified = original.toBuilder()
                .content("Modified content")
                .title("Modified Title")
                .build();

        // Original should be unchanged
        assertThat(original.getContent()).isEqualTo("Original content");
        assertThat(original.getTitle()).isEqualTo("Original Title");
        assertThat(original.getName()).isEqualTo("original.txt");

        // Modified should have new values
        assertThat(modified.getContent()).isEqualTo("Modified content");
        assertThat(modified.getTitle()).isEqualTo("Modified Title");
        assertThat(modified.getName()).isEqualTo("original.txt"); // Unchanged from original
    }

    @Test
    void testToString() {
        TurFileAttributes attributes = TurFileAttributes.builder()
                .content("Test content")
                .name("test.txt")
                .title("Test File")
                .extension("txt")
                .build();

        String toString = attributes.toString();

        assertThat(toString)
                .contains("content=Test content")
                .contains("name=test.txt")
                .contains("title=Test File")
                .contains("extension=txt");
    }

    @Test
    void testMetadataManipulation() {
        TurFileAttributes attributes = new TurFileAttributes();

        // Add metadata
        attributes.getMetadata().put("format", "PDF");
        attributes.getMetadata().put("pages", "10");

        assertThat(attributes.getMetadata()).hasSize(2);
        assertThat(attributes.getMetadata()).containsEntry("format", "PDF");
        assertThat(attributes.getMetadata()).containsEntry("pages", "10");

        // Update metadata
        attributes.getMetadata().put("pages", "12");

        assertThat(attributes.getMetadata()).hasSize(2);
        assertThat(attributes.getMetadata()).containsEntry("pages", "12");

        // Remove metadata
        attributes.getMetadata().remove("format");

        assertThat(attributes.getMetadata()).hasSize(1);
        assertThat(attributes.getMetadata()).doesNotContainKey("format");
        assertThat(attributes.getMetadata()).containsEntry("pages", "12");
    }

    @Test
    void testDefaultValues() {
        TurFileAttributes attributes = TurFileAttributes.builder().build();

        // Check that default values are properly set
        assertThat(attributes.getSize()).isNotNull();
        assertThat(attributes.getLastModified()).isNotNull();
        assertThat(attributes.getMetadata()).isNotNull();
        assertThat(attributes.getMetadata()).isEmpty();

        // Check that lastModified is recent
        long timeDiff = System.currentTimeMillis() - attributes.getLastModified().getTime();
        assertThat(timeDiff).isLessThan(1000); // Should be created within last second
    }
}