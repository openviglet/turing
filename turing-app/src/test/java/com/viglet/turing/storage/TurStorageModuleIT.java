/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.viglet.turing.shared.infra.TurSearchEngineReadOnlySource;
import com.viglet.turing.system.TurCodeInterpreterDockerProbe;
import com.viglet.turing.system.TurDefaultRagReadiness;
import com.viglet.turing.tenant.TurTenantUpgradeTarget;
import com.viglet.turing.testutil.AbstractTuringModuleIT;
import com.viglet.turing.testutil.TuringModuleTest;

/**
 * T1325 — the storage module as a slice: booted with only the modules it declares
 * (shared, system, tenant), the filesystem storage stores, lists and reads back an
 * asset through the tenant-scoped {@link TurStorageService}. Tenant is named in
 * {@code extraIncludes} because storage takes the tenant context in a {@code @Bean}
 * method, which Spring Modulith does not count as a bootstrap dependency.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
@TuringModuleTest(extraIncludes = "tenant")
class TurStorageModuleIT extends AbstractTuringModuleIT {

    @DynamicPropertySource
    static void storageInATemporaryFolder(DynamicPropertyRegistry registry) throws Exception {
        Path folder = Files.createTempDirectory("turing-storage-slice-");
        registry.add("turing.storage.type", () -> "FILESYSTEM");
        registry.add("turing.storage.filesystem.path", () -> folder.toString().replace('\\', '/'));
    }

    // SPIs the lower modules in the slice declare and modules above storage implement:
    // outside the slice, so stood in.
    @MockitoBean
    private TurSearchEngineReadOnlySource searchEngineReadOnlySource;

    @MockitoBean
    private TurDefaultRagReadiness defaultRagReadiness;

    @MockitoBean
    private TurCodeInterpreterDockerProbe codeInterpreterDockerProbe;

    @MockitoBean
    private TurTenantUpgradeTarget tenantUpgradeTarget;

    @Autowired
    private TurStorageService storage;

    @Test
    void anUploadedAssetIsListedAndReadBack() throws Exception {
        assertThat(storage.isEnabled()).isTrue();
        storage.uploadObject(new MockMultipartFile("file", "note.txt", "text/plain",
                "hello slice".getBytes(StandardCharsets.UTF_8)), "slice/");

        assertThat(storage.listObjects("slice/")).anySatisfy(item -> assertThat(item.name()).endsWith("note.txt"));
        try (InputStream in = storage.downloadObject("slice/note.txt")) {
            assertThat(new String(in.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("hello slice");
        }
    }
}
