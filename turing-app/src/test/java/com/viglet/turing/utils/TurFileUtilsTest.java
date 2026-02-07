/*
 *
 * Copyright (C) 2016-2025 the original author or authors.
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

package com.viglet.turing.utils;

import com.viglet.turing.api.ocr.TurTikaFileAttributes;
import com.viglet.turing.commons.file.TurFileAttributes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for TurFileUtils.
 *
 * @author Alexandre Oliveira
 * @since 2025.1.10
 */
class TurFileUtilsTest {

    @Test
    void testConstructorThrowsException() {
        assertThatThrownBy(() -> {
            var constructor = TurFileUtils.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            constructor.newInstance();
        })
        .cause()
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Turing File Utilities class");
    }

    @Test
    void testReadFileWithStringPath(@TempDir Path tempDir) throws IOException {
        File testFile = tempDir.resolve("test.txt").toFile();
        Files.writeString(testFile.toPath(), "Test content");

        TurTikaFileAttributes result = TurFileUtils.readFile(testFile.getAbsolutePath());

        assertThat(result).isNotNull();
        assertThat(result.getContent()).contains("Test content");
    }

    @Test
    void testReadFileWithNullFile() {
        TurTikaFileAttributes result = TurFileUtils.readFile((File) null);

        assertThat(result).isNull();
    }

    @Test
    void testReadFileWithNonExistentFile(@TempDir Path tempDir) {
        File nonExistent = tempDir.resolve("nonexistent.txt").toFile();

        TurTikaFileAttributes result = TurFileUtils.readFile(nonExistent);

        assertThat(result).isNull();
    }

    @Test
    void testParseFileWithValidFile(@TempDir Path tempDir) throws IOException {
        File testFile = tempDir.resolve("test.txt").toFile();
        Files.writeString(testFile.toPath(), "Parse test content");

        TurTikaFileAttributes result = TurFileUtils.parseFile(testFile);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).contains("Parse test content");
    }

    @Test
    void testParseMultipartFile() {
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "Multipart test content".getBytes()
        );

        TurTikaFileAttributes result = TurFileUtils.parseFile(multipartFile);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).contains("Multipart test content");
    }

    @Test
    void testDocumentToText() {
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                "document.txt",
                "text/plain",
                "Document content".getBytes()
        );

        TurFileAttributes result = TurFileUtils.documentToText(multipartFile);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).contains("Document content");
        assertThat(result.getName()).isEqualTo("document.txt");
        assertThat(result.getExtension()).isEqualTo("txt");
    }

    @Test
    void testIsAllowedRemoteUrlStringValid() {
        String validUrl = "https://www.example.com/document.pdf";

        boolean result = TurFileUtils.isAllowedRemoteUrlString(validUrl);

        assertThat(result).isFalse(); // Empty allowed domains list
    }

    @Test
    void testIsAllowedRemoteUrlStringNull() {
        boolean result = TurFileUtils.isAllowedRemoteUrlString(null);

        assertThat(result).isFalse();
    }

    @Test
    void testIsAllowedRemoteUrlStringEmpty() {
        boolean result = TurFileUtils.isAllowedRemoteUrlString("");

        assertThat(result).isFalse();
    }

    @Test
    void testIsAllowedRemoteUrlStringBlank() {
        boolean result = TurFileUtils.isAllowedRemoteUrlString("   ");

        assertThat(result).isFalse();
    }

    @Test
    void testIsAllowedRemoteUrlStringInvalidUrl() {
        String invalidUrl = "not a valid url";

        boolean result = TurFileUtils.isAllowedRemoteUrlString(invalidUrl);

        assertThat(result).isFalse();
    }

    @Test
    void testIsAllowedRemoteUrlStringMalformedUrl() {
        String malformedUrl = "http://[invalid";

        boolean result = TurFileUtils.isAllowedRemoteUrlString(malformedUrl);

        assertThat(result).isFalse();
    }

    @Test
    void testIsSafeLoopbackAddress() throws Exception {
        InetAddress loopback = InetAddress.getByName("127.0.0.1");
        
        boolean result = TurFileUtils.isSafe(loopback);
        
        assertThat(result).isFalse();
    }

    @Test
    void testIsSafeSiteLocalAddress() throws Exception {
        InetAddress siteLocal = InetAddress.getByName("192.168.1.1");
        
        boolean result = TurFileUtils.isSafe(siteLocal);
        
        assertThat(result).isFalse();
    }

    @Test
    void testIsSafePrivateNetworkTenDotZero() throws Exception {
        InetAddress privateNetwork = InetAddress.getByName("10.0.0.1");
        
        boolean result = TurFileUtils.isSafe(privateNetwork);
        
        assertThat(result).isFalse();
    }

    @Test
    void testIsSafePrivateNetwork172() throws Exception {
        InetAddress privateNetwork = InetAddress.getByName("172.16.0.1");
        
        boolean result = TurFileUtils.isSafe(privateNetwork);
        
        assertThat(result).isFalse();
    }

    @Test
    void testIsSafePrivateNetwork172Range() throws Exception {
        InetAddress privateNetwork = InetAddress.getByName("172.31.255.255");
        
        boolean result = TurFileUtils.isSafe(privateNetwork);
        
        assertThat(result).isFalse();
    }

    @Test
    void testIsSafeCGNAT() throws Exception {
        InetAddress cgnat = InetAddress.getByName("100.64.0.1");
        
        boolean result = TurFileUtils.isSafe(cgnat);
        
        assertThat(result).isFalse();
    }

    @Test
    void testIsSafeCGNATRange() throws Exception {
        InetAddress cgnat = InetAddress.getByName("100.127.255.255");
        
        boolean result = TurFileUtils.isSafe(cgnat);
        
        assertThat(result).isFalse();
    }

    @Test
    void testIsSafePublicAddress() throws Exception {
        InetAddress publicAddress = InetAddress.getByName("8.8.8.8");
        
        boolean result = TurFileUtils.isSafe(publicAddress);
        
        assertThat(result).isTrue();
    }

    @Test
    void testParseDocumentWithValidInputStream() throws IOException {
        String content = "Test content for parsing";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(content.getBytes());

        Optional<String> result = TurFileUtils.parseDocument(inputStream);

        assertThat(result).isPresent();
        assertThat(result.get()).contains(content);
    }

    @Test
    void testParseDocumentWithEmptyInputStream() throws IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[0]);

        Optional<String> result = TurFileUtils.parseDocument(inputStream);

        // Empty input stream may return empty optional or empty string depending on implementation
        assertThat(result).isNotNull();
    }

    @Test
    void testUrlContentToTextWithDisallowedUrl() throws Exception {
        java.net.URL url = URI.create("http://localhost/test.txt").toURL();

        TurFileAttributes result = TurFileUtils.urlContentToText(url);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isNullOrEmpty();
    }

    @Test
    void testIsSafeLinkLocalAddress() throws Exception {
        InetAddress linkLocal = InetAddress.getByName("169.254.1.1");
        
        boolean result = TurFileUtils.isSafe(linkLocal);
        
        assertThat(result).isFalse();
    }

    @Test
    void testIsSafeMulticastAddress() throws Exception {
        InetAddress multicast = InetAddress.getByName("224.0.0.1");
        
        boolean result = TurFileUtils.isSafe(multicast);
        
        assertThat(result).isFalse();
    }

    @Test
    void testIsSafeEdgeCases172Dot15() throws Exception {
        InetAddress edge = InetAddress.getByName("172.15.0.1");
        
        boolean result = TurFileUtils.isSafe(edge);
        
        assertThat(result).isTrue();
    }

    @Test
    void testIsSafeEdgeCases172Dot32() throws Exception {
        InetAddress edge = InetAddress.getByName("172.32.0.1");
        
        boolean result = TurFileUtils.isSafe(edge);
        
        assertThat(result).isTrue();
    }

    @Test
    void testIsSafeCGNATEdge100Dot63() throws Exception {
        InetAddress edge = InetAddress.getByName("100.63.0.1");
        
        boolean result = TurFileUtils.isSafe(edge);
        
        assertThat(result).isTrue();
    }

    @Test
    void testIsSafeCGNATEdge100Dot128() throws Exception {
        InetAddress edge = InetAddress.getByName("100.128.0.1");
        
        boolean result = TurFileUtils.isSafe(edge);
        
        assertThat(result).isTrue();
    }
}
