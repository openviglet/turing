/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.auth.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import com.viglet.turing.auth.persistence.dto.TurRoleDto;
import com.viglet.turing.auth.persistence.mapper.TurRoleMapper;
import com.viglet.turing.auth.persistence.model.TurRole;
import com.viglet.turing.auth.persistence.repository.TurRoleRepository;
import com.viglet.turing.shared.exception.TurConflictException;

/**
 * T1319 — a role save made from an older version is refused; analytics
 * intents and users run the same check.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
class TurRoleAPIVersionTest {

    private final TurRoleRepository repository = mock(TurRoleRepository.class);
    private final TurRoleAPI api = new TurRoleAPI(repository, Mappers.getMapper(TurRoleMapper.class));

    private static TurRole stored(long version) {
        TurRole role = new TurRole("Editor");
        role.setId("role-1");
        role.setVersion(version);
        return role;
    }

    @Test
    void aSaveFromAnOlderVersionIsRefused() {
        when(repository.findById("role-1")).thenReturn(Optional.of(stored(4L)));
        TurRoleDto stale = new TurRoleDto();
        stale.setName("Mine");
        stale.setVersion(3L);

        assertThatThrownBy(() -> api.turRoleUpdate("role-1", stale)).isInstanceOf(TurConflictException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void aCreateIgnoresAVersionInThePayload() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        TurRoleDto fresh = new TurRoleDto();
        fresh.setName("New");
        fresh.setVersion(9L);

        TurRoleDto answer = api.turRoleAdd(fresh);

        assertThat(answer.getVersion()).isNull();
    }
}
