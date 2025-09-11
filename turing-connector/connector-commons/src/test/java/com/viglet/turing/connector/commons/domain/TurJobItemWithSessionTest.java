/*
 * Copyright (C) 2016-2025 the original author or authors.
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.viglet.turing.connector.commons.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import com.viglet.turing.client.sn.job.TurSNJobItem;
import com.viglet.turing.connector.commons.TurConnectorSession;

/**
 * Unit tests for TurJobItemWithSession record.
 *
 * @author Alexandre Oliveira
 * @since 2025.3
 */
class TurJobItemWithSessionTest {

        @Test
        void testRecordCreationWithAllFields() {
                TurSNJobItem jobItem = Mockito.mock(TurSNJobItem.class);
                TurConnectorSession session = new TurConnectorSession("test-source",
                                Arrays.asList("site1", "site2"), "test-provider", Locale.ENGLISH);
                Set<String> dependencies = new HashSet<>(Arrays.asList("dep1", "dep2"));
                boolean standalone = true;

                TurJobItemWithSession jobItemWithSession = new TurJobItemWithSession(jobItem,
                                session, dependencies, standalone);

                assertThat(jobItemWithSession.turSNJobItem()).isEqualTo(jobItem);
                assertThat(jobItemWithSession.session()).isEqualTo(session);
                assertThat(jobItemWithSession.dependencies()).isEqualTo(dependencies);
                assertThat(jobItemWithSession.standalone()).isTrue();
        }

        @Test
        void testRecordCreationWithNullValues() {
                TurJobItemWithSession jobItemWithSession =
                                new TurJobItemWithSession(null, null, null, false);

                assertThat(jobItemWithSession.turSNJobItem()).isNull();
                assertThat(jobItemWithSession.session()).isNull();
                assertThat(jobItemWithSession.dependencies()).isNull();
                assertThat(jobItemWithSession.standalone()).isFalse();
        }

        @Test
        void testRecordCreationWithEmptyDependencies() {
                TurSNJobItem jobItem = Mockito.mock(TurSNJobItem.class);
                when(jobItem.getId()).thenReturn("test-id");

                TurConnectorSession session = new TurConnectorSession("test-source",
                                Collections.singletonList("site1"), "test-provider", Locale.FRENCH);
                Set<String> dependencies = Collections.emptySet();
                boolean standalone = false;

                TurJobItemWithSession jobItemWithSession = new TurJobItemWithSession(jobItem,
                                session, dependencies, standalone);

                assertThat(jobItemWithSession.turSNJobItem()).isEqualTo(jobItem);
                assertThat(jobItemWithSession.session()).isEqualTo(session);
                assertThat(jobItemWithSession.dependencies()).isEmpty();
                assertThat(jobItemWithSession.standalone()).isFalse();
        }

        @Test
        void testRecordEquality() {
                TurSNJobItem jobItem1 = Mockito.mock(TurSNJobItem.class);
                TurSNJobItem jobItem2 = Mockito.mock(TurSNJobItem.class);

                TurConnectorSession session = new TurConnectorSession("test-source",
                                Arrays.asList("site1"), "test-provider", Locale.ENGLISH);
                Set<String> dependencies = new HashSet<>(Arrays.asList("dep1"));

                TurJobItemWithSession item1 =
                                new TurJobItemWithSession(jobItem1, session, dependencies, true);
                TurJobItemWithSession item2 =
                                new TurJobItemWithSession(jobItem1, session, dependencies, true);
                TurJobItemWithSession item3 =
                                new TurJobItemWithSession(jobItem2, session, dependencies, true);

                assertThat(item1).isEqualTo(item2);
                assertThat(item1).isNotEqualTo(item3);
                assertThat(item1.hashCode()).isEqualTo(item2.hashCode());
        }

        @Test
        void testRecordToString() {
                TurSNJobItem jobItem = Mockito.mock(TurSNJobItem.class);
                when(jobItem.toString()).thenReturn("MockJobItem");

                TurConnectorSession session = new TurConnectorSession("test-source",
                                Arrays.asList("site1"), "test-provider", Locale.ENGLISH);
                Set<String> dependencies = new HashSet<>(Arrays.asList("dep1"));

                TurJobItemWithSession jobItemWithSession =
                                new TurJobItemWithSession(jobItem, session, dependencies, true);

                String toString = jobItemWithSession.toString();

                assertThat(toString).contains("TurJobItemWithSession");
                assertThat(toString).contains("true");
        }

        @Test
        void testRecordAccessors() {
                TurSNJobItem jobItem = Mockito.mock(TurSNJobItem.class);
                TurConnectorSession session = new TurConnectorSession("test-source",
                                Arrays.asList("site1", "site2"), "test-provider", Locale.GERMAN);
                Set<String> dependencies = new HashSet<>(Arrays.asList("dep1", "dep2", "dep3"));
                boolean standalone = true;

                TurJobItemWithSession jobItemWithSession = new TurJobItemWithSession(jobItem,
                                session, dependencies, standalone);

                // Test accessor methods
                assertThat(jobItemWithSession.turSNJobItem()).isSameAs(jobItem);
                assertThat(jobItemWithSession.session()).isSameAs(session);
                assertThat(jobItemWithSession.dependencies()).isSameAs(dependencies);
                assertThat(jobItemWithSession.standalone()).isEqualTo(standalone);
        }
}
