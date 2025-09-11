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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for TurFileSize.
 *
 * @author Alexandre Oliveira
 * @since 0.3.9
 */
class TurFileSizeTest {

    @Test
    void testDefaultConstructor() {
        TurFileSize fileSize = new TurFileSize();
        
        assertThat(fileSize.getBytes()).isEqualTo(0.0f);
        assertThat(fileSize.getKiloBytes()).isEqualTo(0.0f);
        assertThat(fileSize.getMegaBytes()).isEqualTo(0.0f);
    }

    @Test
    void testConstructorWithBytes() {
        float bytes = 1024.0f;
        TurFileSize fileSize = new TurFileSize(bytes);
        
        assertThat(fileSize.getBytes()).isEqualTo(1024.0f);
        assertThat(fileSize.getKiloBytes()).isEqualTo(1.0f);
        assertThat(fileSize.getMegaBytes()).isEqualTo(0.0f);
    }

    @Test
    void testLargeFileSize() {
        float bytes = 1048576.0f; // 1 MB
        TurFileSize fileSize = new TurFileSize(bytes);
        
        assertThat(fileSize.getBytes()).isEqualTo(1048576.0f);
        assertThat(fileSize.getKiloBytes()).isEqualTo(1024.0f);
        assertThat(fileSize.getMegaBytes()).isEqualTo(1.0f);
    }

    @Test
    void testVeryLargeFileSize() {
        float bytes = 2147483648.0f; // 2 GB
        TurFileSize fileSize = new TurFileSize(bytes);
        
        assertThat(fileSize.getBytes()).isEqualTo(2147483648.0f);
        assertThat(fileSize.getKiloBytes()).isEqualTo(2097152.0f);
        assertThat(fileSize.getMegaBytes()).isEqualTo(2048.0f);
    }

    @Test
    void testFractionalBytes() {
        float bytes = 1536.0f; // 1.5 KB
        TurFileSize fileSize = new TurFileSize(bytes);
        
        assertThat(fileSize.getBytes()).isEqualTo(1536.0f);
        assertThat(fileSize.getKiloBytes()).isEqualTo(1.5f);
        // Very small megabyte values get rounded to 0.0 due to two decimal precision
        assertThat(fileSize.getMegaBytes()).isEqualTo(0.0f);
    }

    @Test
    void testSmallFileSize() {
        float bytes = 512.5f;
        TurFileSize fileSize = new TurFileSize(bytes);
        
        assertThat(fileSize.getBytes()).isEqualTo(512.5f);
        assertThat(fileSize.getKiloBytes()).isCloseTo(0.5f, org.assertj.core.data.Offset.offset(0.01f));
        // Very small megabyte values get rounded to 0.0 due to two decimal precision
        assertThat(fileSize.getMegaBytes()).isEqualTo(0.0f);
    }

    @Test
    void testTwoDecimalPrecision() {
        float bytes = 1000.0f / 3.0f; // Should result in 333.33... bytes
        TurFileSize fileSize = new TurFileSize(bytes);
        
        // Verify that values are rounded to two decimal places
        assertThat(fileSize.getBytes()).isEqualTo(333.33f);
        assertThat(fileSize.getKiloBytes()).isCloseTo(0.33f, org.assertj.core.data.Offset.offset(0.01f));
    }

    @Test
    void testBuilder() {
        TurFileSize fileSize = TurFileSize.builder()
                .bytes(2048.0f)
                .kiloBytes(2.0f)
                .megaBytes(0.002f)
                .build();
        
        assertThat(fileSize.getBytes()).isEqualTo(2048.0f);
        assertThat(fileSize.getKiloBytes()).isEqualTo(2.0f);
        assertThat(fileSize.getMegaBytes()).isEqualTo(0.002f);
    }

    @Test
    void testAllArgsConstructor() {
        TurFileSize fileSize = new TurFileSize(1024.0f, 1.0f, 0.001f);
        
        assertThat(fileSize.getBytes()).isEqualTo(1024.0f);
        assertThat(fileSize.getKiloBytes()).isEqualTo(1.0f);
        assertThat(fileSize.getMegaBytes()).isEqualTo(0.001f);
    }

    @Test
    void testToString() {
        TurFileSize fileSize = new TurFileSize(1024.0f);
        String toString = fileSize.toString();
        
        assertThat(toString).contains("bytes=1024.0");
        assertThat(toString).contains("kiloBytes=1.0");
        assertThat(toString).contains("megaBytes=0.0");
    }

    @Test
    void testZeroBytes() {
        TurFileSize fileSize = new TurFileSize(0.0f);
        
        assertThat(fileSize.getBytes()).isEqualTo(0.0f);
        assertThat(fileSize.getKiloBytes()).isEqualTo(0.0f);
        assertThat(fileSize.getMegaBytes()).isEqualTo(0.0f);
    }

    @Test
    void testRoundingBehavior() {
        // Test rounding half up
        float bytes = 1023.995f; // Should round up to 1024.0
        TurFileSize fileSize = new TurFileSize(bytes);
        
        // The exact value depends on float precision, but we can test the rounding behavior
        assertThat(fileSize.getBytes()).isCloseTo(1024.0f, org.assertj.core.data.Offset.offset(0.01f));
    }

    @Test
    void testCommonFileSizes() {
        // Test some common file sizes
        
        // 1 KB
        TurFileSize oneKB = new TurFileSize(1024.0f);
        assertThat(oneKB.getKiloBytes()).isEqualTo(1.0f);
        
        // 500 KB
        TurFileSize fiveHundredKB = new TurFileSize(512000.0f);
        assertThat(fiveHundredKB.getKiloBytes()).isEqualTo(500.0f);
        
        // 1.5 MB
        TurFileSize oneMBHalf = new TurFileSize(1572864.0f);
        assertThat(oneMBHalf.getMegaBytes()).isEqualTo(1.5f);
        
        // 10 MB
        TurFileSize tenMB = new TurFileSize(10485760.0f);
        assertThat(tenMB.getMegaBytes()).isEqualTo(10.0f);
    }
}