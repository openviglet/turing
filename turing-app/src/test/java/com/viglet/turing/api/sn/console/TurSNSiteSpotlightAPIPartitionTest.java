/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.api.sn.console;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.viglet.turing.commons.se.TurSEPartition;
import com.viglet.turing.persistence.dto.sn.spotlight.TurSNSiteSpotlightDto;
import com.viglet.turing.persistence.mapper.sn.spotlight.TurSNSiteSpotlightMapper;
import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.persistence.model.sn.spotlight.TurSNSiteSpotlight;
import com.viglet.turing.persistence.repository.sn.TurSNSiteRepository;
import com.viglet.turing.persistence.repository.sn.spotlight.TurSNSiteSpotlightDocumentRepository;
import com.viglet.turing.persistence.repository.sn.spotlight.TurSNSiteSpotlightRepository;
import com.viglet.turing.persistence.repository.sn.spotlight.TurSNSiteSpotlightTermRepository;
import com.viglet.turing.spring.security.auth.TurApiKeyPartitionContext;
import com.viglet.turing.system.cache.TurCacheEvictionScheduler;

/**
 * T1209 — a partitioned key sees, changes and deletes only its partition's own
 * spotlights, and the ones it creates are its partition's.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
class TurSNSiteSpotlightAPIPartitionTest {

    private static final TurSEPartition A = new TurSEPartition("site_key", "a.example.org");
    private static final TurSEPartition B = new TurSEPartition("site_key", "b.example.org");

    private final TurSNSiteRepository siteRepository = mock(TurSNSiteRepository.class);
    private final TurSNSiteSpotlightRepository spotlightRepository = mock(TurSNSiteSpotlightRepository.class);
    private final TurSNSiteSpotlightAPI api = new TurSNSiteSpotlightAPI(siteRepository, spotlightRepository,
            mock(TurSNSiteSpotlightDocumentRepository.class), mock(TurSNSiteSpotlightTermRepository.class),
            Mappers.getMapper(TurSNSiteSpotlightMapper.class),
            new TurCacheEvictionScheduler(new ConcurrentMapCacheManager()));

    @AfterEach
    void unbind() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void aPartitionedKeyListsOnlyItsOwnSpotlights() {
        TurSNSite site = new TurSNSite();
        when(siteRepository.findById("site")).thenReturn(Optional.of(site));
        when(spotlightRepository.findByTurSNSite(any(), eq(site)))
                .thenReturn(List.of(spotlight("own", A), spotlight("other", B), spotlight("siteWide", null)));
        bind(A);

        List<TurSNSiteSpotlightDto> result = api.turSNSiteSpotlightList("site");

        assertThat(result).extracting(TurSNSiteSpotlightDto::getName).containsExactly("own");
    }

    @Test
    void anUnpartitionedKeyListsEverySpotlight() {
        TurSNSite site = new TurSNSite();
        when(siteRepository.findById("site")).thenReturn(Optional.of(site));
        when(spotlightRepository.findByTurSNSite(any(), eq(site)))
                .thenReturn(List.of(spotlight("own", A), spotlight("siteWide", null)));

        assertThat(api.turSNSiteSpotlightList("site")).hasSize(2);
    }

    @Test
    void aPartitionedKeyCannotDeleteAnotherPartitionsSpotlight() {
        when(spotlightRepository.findById("sp")).thenReturn(Optional.of(spotlight("other", B)));
        bind(A);

        assertThat(api.turSNSiteSpotlightDelete("sp", "site")).isFalse();
        verify(spotlightRepository, never()).deleteById("sp");
    }

    @Test
    void aPartitionedKeyCannotDeleteASiteWideSpotlight() {
        when(spotlightRepository.findById("sp")).thenReturn(Optional.of(spotlight("siteWide", null)));
        bind(A);

        assertThat(api.turSNSiteSpotlightDelete("sp", "site")).isFalse();
        verify(spotlightRepository, never()).deleteById("sp");
    }

    @Test
    void aPartitionedKeyDeletesItsOwnSpotlight() {
        when(spotlightRepository.findById("sp")).thenReturn(Optional.of(spotlight("own", A)));
        bind(A);

        assertThat(api.turSNSiteSpotlightDelete("sp", "site")).isTrue();
        verify(spotlightRepository).deleteById("sp");
    }

    @Test
    void aPartitionedKeyReadsNoOtherPartitionsSpotlight() {
        when(spotlightRepository.findById("sp")).thenReturn(Optional.of(spotlight("other", B)));
        bind(A);

        assertThat(api.turSNSiteSpotlightGet("site", "sp").getName()).isNull();
    }

    @Test
    void aSpotlightAPartitionedKeyCreatesIsItsPartitions() {
        bind(A);
        TurSNSiteSpotlightDto dto = new TurSNSiteSpotlightDto();
        dto.setName("promo");

        api.turSNSiteSpotlightAdd(dto, "site");

        verify(spotlightRepository).save(org.mockito.ArgumentMatchers.argThat(
                saved -> saved.partition().equals(Optional.of(A))));
    }

    @Test
    void aSpotlightAnUnpartitionedKeyCreatesIsSiteWide() {
        TurSNSiteSpotlightDto dto = new TurSNSiteSpotlightDto();
        dto.setName("promo");

        api.turSNSiteSpotlightAdd(dto, "site");

        verify(spotlightRepository).save(org.mockito.ArgumentMatchers.argThat(
                saved -> saved.partition().isEmpty()));
    }

    private static TurSNSiteSpotlight spotlight(String name, TurSEPartition partition) {
        TurSNSiteSpotlight spotlight = new TurSNSiteSpotlight();
        spotlight.setName(name);
        spotlight.bindTo(partition);
        return spotlight;
    }

    private static void bind(TurSEPartition partition) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(TurApiKeyPartitionContext.class.getName(), partition);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }
}
