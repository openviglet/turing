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

package com.viglet.turing.commons.utils;

import org.apache.commons.collections4.KeyValue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TurCommonsUtils.
 *
 * @author Alexandre Oliveira
 * @since 0.3.6
 */
class TurCommonsUtilsTest {

    @Test
    void testGetKeyValueFromColonValid() {
        String input = "key:value";
        Optional<KeyValue<String, String>> result = TurCommonsUtils.getKeyValueFromColon(input);
        
        assertThat(result).isPresent();
        assertThat(result.get().getKey()).isEqualTo("key");
        assertThat(result.get().getValue()).isEqualTo("value");
    }

    @Test
    void testGetKeyValueFromColonWithMultipleColons() {
        String input = "key:value:extra";
        Optional<KeyValue<String, String>> result = TurCommonsUtils.getKeyValueFromColon(input);
        
        assertThat(result).isPresent();
        assertThat(result.get().getKey()).isEqualTo("key");
        assertThat(result.get().getValue()).isEqualTo("value:extra");
    }

    @Test
    void testGetKeyValueFromColonNoColon() {
        String input = "keyvalue";
        Optional<KeyValue<String, String>> result = TurCommonsUtils.getKeyValueFromColon(input);
        
        assertThat(result).isEmpty();
    }

    @Test
    void testGetKeyValueFromColonEmptyValue() {
        String input = "key:";
        Optional<KeyValue<String, String>> result = TurCommonsUtils.getKeyValueFromColon(input);
        
        // Based on the actual implementation, this returns empty when there's only one part after split
        assertThat(result).isEmpty();
    }

    @Test
    void testIsValidUrlValidUrl() throws MalformedURLException {
        URL validUrl = new URL("https://www.example.com");
        boolean result = TurCommonsUtils.isValidUrl(validUrl);
        
        assertThat(result).isTrue();
    }

    @Test
    void testIsValidUrlLocalUrl() throws MalformedURLException {
        URL localUrl = new URL("http://localhost:8080/api");
        boolean result = TurCommonsUtils.isValidUrl(localUrl);
        
        assertThat(result).isTrue();
    }

    @Test
    void testIsValidUrlFileUrl() throws MalformedURLException {
        URL fileUrl = new URL("file:///tmp/test.txt");
        boolean result = TurCommonsUtils.isValidUrl(fileUrl);
        
        // Based on the actual behavior, file URLs are considered invalid by the validator
        assertThat(result).isFalse();
    }

    @Test
    void testHtml2Text() {
        String html = "<p>This is a <strong>test</strong> with <em>HTML</em> tags.</p>";
        String result = TurCommonsUtils.html2Text(html);
        
        assertThat(result).isEqualTo("This is a test with HTML tags.");
    }

    @Test
    void testHtml2TextWithComplexHtml() {
        String html = "<div><h1>Title</h1><p>Paragraph with <a href=\"#\">link</a></p></div>";
        String result = TurCommonsUtils.html2Text(html);
        
        assertThat(result).isEqualTo("Title Paragraph with link");
    }

    @Test
    void testText2DescriptionShortText() {
        String text = "Short text";
        String result = TurCommonsUtils.text2Description(text, 100);
        
        assertThat(result).isEqualTo("Short text ...");
    }

    @Test
    void testText2DescriptionLongText() {
        String text = "This is a very long text that should be truncated at word boundaries " +
                     "to ensure readability and proper formatting when displayed.";
        String result = TurCommonsUtils.text2Description(text, 50);
        
        assertThat(result).contains("...");
        assertThat(result.length()).isLessThanOrEqualTo(52); // 50 + " ..."
    }

    @Test
    void testText2DescriptionNullText() {
        String result = TurCommonsUtils.text2Description(null, 50);
        
        assertThat(result).isEqualTo("null ...");
    }

    @Test
    void testHtml2Description() {
        String html = "<p>This is HTML <strong>content</strong> that should be converted to text and truncated.</p>";
        String result = TurCommonsUtils.html2Description(html, 30);
        
        assertThat(result).contains("This is HTML content");
        assertThat(result).contains("...");
    }

    @Test
    void testAddOrReplaceParameterWithLocale() throws URISyntaxException {
        URI uri = new URI("/test?param1=value1");
        Locale locale = Locale.ENGLISH;
        URI result = TurCommonsUtils.addOrReplaceParameter(uri, "locale", locale, false);
        
        assertThat(result.toString()).contains("locale=en");
    }

    @Test
    void testAddOrReplaceParameterNewParameter() throws URISyntaxException {
        URI uri = new URI("/test?param1=value1");
        URI result = TurCommonsUtils.addOrReplaceParameter(uri, "newParam", "newValue", false);
        
        assertThat(result.toString()).contains("newParam=newValue");
        assertThat(result.toString()).contains("param1=value1");
    }

    @Test
    void testAddOrReplaceParameterExistingParameter() throws URISyntaxException {
        URI uri = new URI("/test?param1=oldValue&param2=value2");
        URI result = TurCommonsUtils.addOrReplaceParameter(uri, "param1", "newValue", false);
        
        assertThat(result.toString()).contains("param1=newValue");
        assertThat(result.toString()).contains("param2=value2");
        assertThat(result.toString()).doesNotContain("param1=oldValue");
    }

    @Test
    void testCleanTextContent() {
        String dirtyText = " \t\n Text  with   multiple\r\n   spaces \t ";
        String result = TurCommonsUtils.cleanTextContent(dirtyText);
        
        assertThat(result).isEqualTo("Text with multiple spaces");
    }

    @Test
    void testCleanTextContentEmpty() {
        String result = TurCommonsUtils.cleanTextContent("");
        
        assertThat(result).isEmpty();
    }

    @Test
    void testCloneListOfTermsAsString() {
        List<Object> input = Arrays.asList("term1", "term2", "term3");
        List<String> result = TurCommonsUtils.cloneListOfTermsAsString(input);
        
        assertThat(result).hasSize(3);
        assertThat(result).containsExactly("term1", "term2", "term3");
    }

    @Test
    void testAddFilesToZip(@TempDir Path tempDir) throws IOException {
        // Create source directory with test files
        File sourceDir = tempDir.resolve("source").toFile();
        assertThat(sourceDir.mkdirs()).isTrue();
        
        File testFile1 = new File(sourceDir, "test1.txt");
        Files.write(testFile1.toPath(), "Test content 1".getBytes());
        
        File testFile2 = new File(sourceDir, "test2.txt");
        Files.write(testFile2.toPath(), "Test content 2".getBytes());
        
        // Create destination zip
        File destinationZip = tempDir.resolve("test.zip").toFile();
        
        // Test the method
        TurCommonsUtils.addFilesToZip(sourceDir, destinationZip);
        
        // Verify zip was created
        assertThat(destinationZip).exists();
        assertThat(destinationZip.length()).isGreaterThan(0);
    }

    @Test
    void testUnZipIt(@TempDir Path tempDir) throws IOException {
        // Create a test file structure
        File sourceDir = tempDir.resolve("source").toFile();
        assertThat(sourceDir.mkdirs()).isTrue();
        
        File testFile = new File(sourceDir, "test.txt");
        Files.write(testFile.toPath(), "Test content".getBytes());
        
        // Create zip
        File zipFile = tempDir.resolve("test.zip").toFile();
        TurCommonsUtils.addFilesToZip(sourceDir, zipFile);
        
        // Create extraction directory
        File extractDir = tempDir.resolve("extract").toFile();
        assertThat(extractDir.mkdirs()).isTrue();
        
        // Test unzipping
        TurCommonsUtils.unZipIt(zipFile, extractDir);
        
        // Verify extracted files
        File extractedFile = new File(extractDir, "test.txt");
        assertThat(extractedFile).exists();
        assertThat(Files.readString(extractedFile.toPath())).isEqualTo("Test content");
    }

    @Test
    void testIsValidJsonValidObject() {
        String validJson = "{\"key\": \"value\", \"number\": 42}";
        boolean result = TurCommonsUtils.isValidJson(validJson);
        
        assertThat(result).isTrue();
    }

    @Test
    void testIsValidJsonValidArray() {
        String validJsonArray = "[{\"key\": \"value1\"}, {\"key\": \"value2\"}]";
        boolean result = TurCommonsUtils.isValidJson(validJsonArray);
        
        assertThat(result).isTrue();
    }

    @Test
    void testIsValidJsonInvalidJson() {
        String invalidJson = "{invalid json structure";
        boolean result = TurCommonsUtils.isValidJson(invalidJson);
        
        assertThat(result).isFalse();
    }

    @Test
    void testAsJsonStringValidObject() throws Exception {
        TestObject testObject = new TestObject("test", 42);
        String result = TurCommonsUtils.asJsonString(testObject);
        
        assertThat(result).contains("\"name\":\"test\"");
        assertThat(result).contains("\"value\":42");
    }

    @Test
    void testAsJsonStringNullObject() throws Exception {
        // Test with null - should return "null" as JSON string
        String result = TurCommonsUtils.asJsonString(null);
        assertThat(result).isEqualTo("null");
    }

    @Test
    void testGetStoreDir() {
        File storeDir = TurCommonsUtils.getStoreDir();
        
        assertThat(storeDir).isNotNull();
        assertThat(storeDir.getName()).isEqualTo("store");
        assertThat(storeDir).exists();
    }

    @Test
    void testAddSubDirToStoreDir() {
        String subDirName = "testSubDir";
        File subDir = TurCommonsUtils.addSubDirToStoreDir(subDirName);
        
        assertThat(subDir).isNotNull();
        assertThat(subDir.getName()).isEqualTo(subDirName);
        assertThat(subDir).exists();
        
        // Clean up
        assertThat(subDir.delete()).isTrue();
    }

    @Test
    void testGetTempDirectory() {
        File tempDir = TurCommonsUtils.getTempDirectory();
        
        assertThat(tempDir).isNotNull();
        assertThat(tempDir.getName()).isEqualTo("tmp");
        assertThat(tempDir).exists();
    }

    // Helper class for JSON testing
    public static class TestObject {
        private final String name;
        private final int value;

        public TestObject(String name, int value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public int getValue() {
            return value;
        }
    }
}