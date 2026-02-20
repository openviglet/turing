/*
 * Copyright (C) 2016-2025 the original author or authors.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.viglet.turing.api.sn.console;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.viglet.turing.commons.se.field.TurSEFieldType;
import com.viglet.turing.persistence.model.se.TurSEInstance;
import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.persistence.model.sn.field.TurSNSiteField;
import com.viglet.turing.persistence.model.sn.field.TurSNSiteFieldExt;
import com.viglet.turing.persistence.model.sn.field.TurSNSiteFieldExtFacet;
import com.viglet.turing.persistence.repository.se.TurSEInstanceRepository;
import com.viglet.turing.persistence.repository.sn.TurSNSiteRepository;
import com.viglet.turing.persistence.repository.sn.field.TurSNSiteFieldExtFacetRepository;
import com.viglet.turing.persistence.repository.sn.field.TurSNSiteFieldExtRepository;
import com.viglet.turing.persistence.repository.sn.field.TurSNSiteFieldRepository;
import com.viglet.turing.persistence.repository.sn.locale.TurSNSiteLocaleRepository;
import com.viglet.turing.sn.TurSNFieldType;
import com.viglet.turing.sn.template.TurSNTemplate;

/**
 * Unit tests for TurSNSiteFieldExtAPI.
 *
 * @author Alexandre Oliveira
 * @since 2026.1.10
 */
class TurSNSiteFieldExtAPITest {

        private static Object invokePrivate(Object target, String methodName, Class<?>[] parameterTypes,
                        Object... args) throws Exception {
                Method method = target.getClass().getDeclaredMethod(methodName, parameterTypes);
                method.setAccessible(true);
                return method.invoke(target, args);
        }

        @Test
        void testFieldExtListReturnsEmptyWhenSiteMissing() {
                TurSNSiteRepository siteRepository = mock(TurSNSiteRepository.class);
                TurSNSiteFieldExtAPI api = new TurSNSiteFieldExtAPI(siteRepository,
                                mock(TurSNSiteFieldExtRepository.class), mock(TurSNSiteFieldExtFacetRepository.class),
                                mock(TurSNSiteFieldRepository.class), mock(TurSNSiteLocaleRepository.class),
                                mock(TurSEInstanceRepository.class),
                                mock(TurSNTemplate.class));

                when(siteRepository.findById("site")).thenReturn(Optional.empty());

                List<TurSNSiteFieldExt> result = api.turSNSiteFieldExtList("site");

                assertThat(result).isEmpty();
        }

        @Test
        void testFieldExtGetReturnsFacetLocales() {
                TurSNSiteFieldExtRepository fieldExtRepository = mock(TurSNSiteFieldExtRepository.class);
                TurSNSiteFieldExtFacetRepository facetRepository = mock(TurSNSiteFieldExtFacetRepository.class);
                TurSNSiteFieldExtAPI api = new TurSNSiteFieldExtAPI(mock(TurSNSiteRepository.class),
                                fieldExtRepository, facetRepository, mock(TurSNSiteFieldRepository.class),
                                mock(TurSNSiteLocaleRepository.class), mock(TurSEInstanceRepository.class),
                                mock(TurSNTemplate.class));

                when(fieldExtRepository.findById("id")).thenReturn(Optional.empty());

                TurSNSiteFieldExt result = api.turSNSiteFieldExtGet("site", "id");

                assertThat(result).isNotNull();
                verify(facetRepository).findByTurSNSiteFieldExt(result);
        }

        @Test
        void testFieldExtUpdateSetsFacetPositionWhenMissing() {
                TurSNSiteFieldExtRepository fieldExtRepository = mock(TurSNSiteFieldExtRepository.class);
                TurSNSiteFieldRepository fieldRepository = mock(TurSNSiteFieldRepository.class);
                TurSNSiteRepository siteRepository = mock(TurSNSiteRepository.class);
                TurSNSiteFieldExtAPI api = new TurSNSiteFieldExtAPI(siteRepository,
                                fieldExtRepository, mock(TurSNSiteFieldExtFacetRepository.class),
                                fieldRepository, mock(TurSNSiteLocaleRepository.class),
                                mock(TurSEInstanceRepository.class), mock(TurSNTemplate.class));
                TurSNSite site = new TurSNSite();
                TurSNSiteFieldExt existing = new TurSNSiteFieldExt();
                existing.setSnType(TurSNFieldType.SE);
                existing.setExternalId("ext");
                existing.setTurSNSite(site);
                TurSNSiteFieldExt payload = new TurSNSiteFieldExt();
                payload.setName("title");
                payload.setFacet(1);
                payload.setSnType(TurSNFieldType.SE);
                payload.setExternalId("ext");
                payload.setTurSNSite(site);
                when(siteRepository.findById("site")).thenReturn(Optional.of(site));
                when(fieldExtRepository.findById("id")).thenReturn(Optional.of(existing));
                when(fieldExtRepository.findMaxFacetPosition(site)).thenReturn(Optional.of(3));
                when(fieldRepository.findById("ext")).thenReturn(Optional.empty());

                TurSNSiteFieldExt result = api.turSNSiteFieldExtUpdate("site", "id", payload);

                assertThat(result).isNotNull();
                assertThat(result.getFacetPosition()).isEqualTo(4);
        }

        @Test
        void testFieldExtUpdateResetsFacetPositionWhenFacetDisabled() {
                TurSNSiteFieldExtRepository fieldExtRepository = mock(TurSNSiteFieldExtRepository.class);
                TurSNSiteRepository siteRepository = mock(TurSNSiteRepository.class);
                TurSNSiteFieldExtAPI api = new TurSNSiteFieldExtAPI(siteRepository,
                                fieldExtRepository, mock(TurSNSiteFieldExtFacetRepository.class),
                                mock(TurSNSiteFieldRepository.class), mock(TurSNSiteLocaleRepository.class),
                                mock(TurSEInstanceRepository.class), mock(TurSNTemplate.class));
                TurSNSite site = new TurSNSite();
                TurSNSiteFieldExt existing = new TurSNSiteFieldExt();
                existing.setFacetPosition(7);
                existing.setSnType(TurSNFieldType.SE);
                TurSNSiteFieldExt payload = new TurSNSiteFieldExt();
                payload.setFacet(0);
                payload.setSnType(TurSNFieldType.SE);

                when(fieldExtRepository.findById("id")).thenReturn(Optional.of(existing));
                when(siteRepository.findById("site")).thenReturn(Optional.of(site));
                when(fieldExtRepository.save(ArgumentMatchers.any(TurSNSiteFieldExt.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                TurSNSiteFieldExt result = api.turSNSiteFieldExtUpdate("site", "id", payload);

                assertThat(result.getFacetPosition()).isZero();
        }

        @Test
        void testFieldExtUpdateUsesProvidedFacetPosition() {
                TurSNSiteFieldExtRepository fieldExtRepository = mock(TurSNSiteFieldExtRepository.class);
                TurSNSiteRepository siteRepository = mock(TurSNSiteRepository.class);
                TurSNSiteFieldExtAPI api = new TurSNSiteFieldExtAPI(siteRepository,
                                fieldExtRepository, mock(TurSNSiteFieldExtFacetRepository.class),
                                mock(TurSNSiteFieldRepository.class), mock(TurSNSiteLocaleRepository.class),
                                mock(TurSEInstanceRepository.class), mock(TurSNTemplate.class));
                TurSNSite site = new TurSNSite();
                TurSNSiteFieldExt existing = new TurSNSiteFieldExt();
                existing.setSnType(TurSNFieldType.SE);
                TurSNSiteFieldExt payload = new TurSNSiteFieldExt();
                payload.setFacet(1);
                payload.setFacetPosition(9);
                payload.setSnType(TurSNFieldType.SE);

                when(fieldExtRepository.findById("id")).thenReturn(Optional.of(existing));
                when(siteRepository.findById("site")).thenReturn(Optional.of(site));
                when(fieldExtRepository.save(ArgumentMatchers.any(TurSNSiteFieldExt.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                TurSNSiteFieldExt result = api.turSNSiteFieldExtUpdate("site", "id", payload);

                assertThat(result.getFacetPosition()).isEqualTo(9);
                verify(fieldExtRepository, never()).findMaxFacetPosition(ArgumentMatchers.any(TurSNSite.class));
        }

        @Test
        void testFieldExtUpdateLinksFacetLocalesToFieldExt() {
                TurSNSiteFieldExtFacetRepository facetRepo = mock(TurSNSiteFieldExtFacetRepository.class);
                TurSNSiteFieldExtRepository fieldExtRepo = mock(TurSNSiteFieldExtRepository.class);
                TurSNSiteRepository siteRepo = mock(TurSNSiteRepository.class);
                TurSNSiteFieldRepository fieldRepo = mock(TurSNSiteFieldRepository.class);

                TurSNSiteFieldExtAPI api = new TurSNSiteFieldExtAPI(
                                siteRepo, fieldExtRepo, facetRepo,
                                fieldRepo, mock(TurSNSiteLocaleRepository.class),
                                mock(TurSEInstanceRepository.class), mock(TurSNTemplate.class));

                TurSNSite site = new TurSNSite();

                TurSNSiteFieldExt existing = new TurSNSiteFieldExt();
                existing.setSnType(TurSNFieldType.SE);

                TurSNSiteFieldExt payload = new TurSNSiteFieldExt();
                payload.setSnType(TurSNFieldType.SE);
                payload.setExternalId("ext-id");

                Set<TurSNSiteFieldExtFacet> payloadFacets = new HashSet<>();
                payloadFacets.add(new TurSNSiteFieldExtFacet());
                payloadFacets.add(new TurSNSiteFieldExtFacet());
                payload.setFacetLocales(payloadFacets);

                when(fieldExtRepo.findById("id")).thenReturn(Optional.of(existing));
                when(siteRepo.findById("site")).thenReturn(Optional.of(site));
                when(fieldExtRepo.findMaxFacetPosition(site)).thenReturn(Optional.of(1));
                when(facetRepo.saveAll(any())).thenAnswer(inv -> {
                        Iterable<?> it = inv.getArgument(0);
                        List<Object> list = new ArrayList<>();
                        it.forEach(list::add);
                        return list;
                });

                when(fieldExtRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
                when(fieldRepo.findById("ext-id")).thenReturn(Optional.of(new TurSNSiteField()));

                TurSNSiteFieldExt result = api.turSNSiteFieldExtUpdate("site", "id", payload);

                assertThat(result.getFacetLocales()).hasSize(2);
                result.getFacetLocales().forEach(f -> assertThat(f.getTurSNSiteFieldExt()).isSameAs(existing));
        }

        @Test
        void testFieldExtDeleteDeletesExternalFieldWhenSE() {
                TurSNSiteFieldExtRepository fieldExtRepository = mock(TurSNSiteFieldExtRepository.class);
                TurSNSiteFieldRepository fieldRepository = mock(TurSNSiteFieldRepository.class);
                TurSNSiteFieldExtAPI api = new TurSNSiteFieldExtAPI(mock(TurSNSiteRepository.class),
                                fieldExtRepository, mock(TurSNSiteFieldExtFacetRepository.class),
                                fieldRepository, mock(TurSNSiteLocaleRepository.class),
                                mock(TurSEInstanceRepository.class),
                                mock(TurSNTemplate.class));
                TurSNSiteFieldExt existing = new TurSNSiteFieldExt();
                existing.setSnType(TurSNFieldType.SE);
                existing.setExternalId("ext");

                when(fieldExtRepository.findById("id")).thenReturn(Optional.of(existing));

                boolean result = api.turSNSiteFieldExtDelete("site", "id");

                assertThat(result).isTrue();
                verify(fieldRepository).delete("ext");
                verify(fieldExtRepository).delete("id");
        }

        @Test
        void testFieldExtAddReturnsDefaultWhenSiteMissing() {
                TurSNSiteRepository siteRepository = mock(TurSNSiteRepository.class);
                TurSNSiteFieldExtAPI api = new TurSNSiteFieldExtAPI(siteRepository,
                                mock(TurSNSiteFieldExtRepository.class), mock(TurSNSiteFieldExtFacetRepository.class),
                                mock(TurSNSiteFieldRepository.class), mock(TurSNSiteLocaleRepository.class),
                                mock(TurSEInstanceRepository.class), mock(TurSNTemplate.class));

                when(siteRepository.findById("site")).thenReturn(Optional.empty());

                TurSNSiteFieldExt result = api.turSNSiteFieldExtAdd("site", new TurSNSiteFieldExt());

                assertThat(result.getId()).isNull();
        }

        @Test
        void testFieldExtStructureSetsSite() {
                TurSNSiteRepository siteRepository = mock(TurSNSiteRepository.class);
                TurSNSiteFieldExtAPI api = new TurSNSiteFieldExtAPI(siteRepository,
                                mock(TurSNSiteFieldExtRepository.class), mock(TurSNSiteFieldExtFacetRepository.class),
                                mock(TurSNSiteFieldRepository.class), mock(TurSNSiteLocaleRepository.class),
                                mock(TurSEInstanceRepository.class), mock(TurSNTemplate.class));
                TurSNSite site = new TurSNSite();

                when(siteRepository.findById("site")).thenReturn(Optional.of(site));

                TurSNSiteFieldExt result = api.turSNSiteFieldExtStructure("site");

                assertThat(result.getTurSNSite()).isSameAs(site);
        }

        @Test
        void testFieldExtAddCreatesSEFieldWhenSiteFound() {
                TurSNSiteRepository siteRepository = mock(TurSNSiteRepository.class);
                TurSNSiteFieldRepository fieldRepository = mock(TurSNSiteFieldRepository.class);
                TurSNSiteFieldExtRepository fieldExtRepository = mock(TurSNSiteFieldExtRepository.class);
                TurSEInstanceRepository instanceRepository = mock(TurSEInstanceRepository.class);
                TurSNSiteFieldExtAPI api = new TurSNSiteFieldExtAPI(siteRepository,
                                fieldExtRepository, mock(TurSNSiteFieldExtFacetRepository.class),
                                fieldRepository, mock(TurSNSiteLocaleRepository.class),
                                instanceRepository, mock(TurSNTemplate.class));
                TurSNSite site = new TurSNSite();
                TurSEInstance instance = new TurSEInstance();
                instance.setId("se-id");
                site.setTurSEInstance(instance);
                site.setTurSNSiteLocales(new HashSet<>());
                TurSNSiteFieldExt payload = new TurSNSiteFieldExt();
                payload.setName("title");
                payload.setType(TurSEFieldType.STRING);

                when(siteRepository.findById("site")).thenReturn(Optional.of(site));
                when(fieldExtRepository.existsByTurSNSiteAndName(site, "title")).thenReturn(false);
                when(fieldRepository.existsByTurSNSiteAndName(site, "title")).thenReturn(false);
                when(instanceRepository.findById("se-id")).thenReturn(Optional.empty());
                when(fieldRepository.save(org.mockito.ArgumentMatchers.any(TurSNSiteField.class)))
                                .thenAnswer(invocation -> {
                                        TurSNSiteField field = invocation.getArgument(0);
                                        field.setId("field-id");
                                        return field;
                                });
                when(fieldExtRepository.save(ArgumentMatchers.any(TurSNSiteFieldExt.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                TurSNSiteFieldExt result = api.turSNSiteFieldExtAdd("site", payload);

                assertThat(result.getSnType()).isEqualTo(TurSNFieldType.SE);
                assertThat(result.getExternalId()).isEqualTo("field-id");
        }

        @Test
        void testFieldExtAddReturnsDefaultWhenNameAlreadyExists() {
                TurSNSiteRepository siteRepository = mock(TurSNSiteRepository.class);
                TurSNSiteFieldRepository fieldRepository = mock(TurSNSiteFieldRepository.class);
                TurSNSiteFieldExtRepository fieldExtRepository = mock(TurSNSiteFieldExtRepository.class);
                TurSNSiteFieldExtAPI api = new TurSNSiteFieldExtAPI(siteRepository,
                                fieldExtRepository, mock(TurSNSiteFieldExtFacetRepository.class),
                                fieldRepository, mock(TurSNSiteLocaleRepository.class),
                                mock(TurSEInstanceRepository.class), mock(TurSNTemplate.class));
                TurSNSite site = new TurSNSite();
                TurSNSiteFieldExt payload = new TurSNSiteFieldExt();
                payload.setName("title");

                when(siteRepository.findById("site")).thenReturn(Optional.of(site));
                when(fieldExtRepository.existsByTurSNSiteAndName(site, "title")).thenReturn(true);

                assertThatThrownBy(() -> api.turSNSiteFieldExtAdd("site", payload))
                                .isInstanceOf(ResponseStatusException.class)
                                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                                .isEqualTo(HttpStatus.CONFLICT);

                verify(fieldRepository, never()).save(ArgumentMatchers.any(TurSNSiteField.class));
                verify(fieldExtRepository, never()).save(ArgumentMatchers.any(TurSNSiteFieldExt.class));
        }

        @Test
        void testBuildAddFieldPayloadUsesStringForMultiValued() throws Exception {
                TurSNSiteFieldExtAPI api = new TurSNSiteFieldExtAPI(mock(TurSNSiteRepository.class),
                                mock(TurSNSiteFieldExtRepository.class), mock(TurSNSiteFieldExtFacetRepository.class),
                                mock(TurSNSiteFieldRepository.class), mock(TurSNSiteLocaleRepository.class),
                                mock(TurSEInstanceRepository.class), mock(TurSNTemplate.class));
                TurSNSiteFieldExt fieldExt = new TurSNSiteFieldExt();
                fieldExt.setMultiValued(1);
                fieldExt.setType(TurSEFieldType.DATE);

                JSONObject payload = (JSONObject) invokePrivate(api, "buildAddFieldPayload",
                                new Class<?>[] { TurSNSiteFieldExt.class, String.class }, fieldExt, "title");

                JSONObject addField = payload.getJSONObject(TurSNSiteFieldExtAPI.ADD_FIELD);
                assertThat(addField.getString(TurSNSiteFieldExtAPI.NAME)).isEqualTo("title");
                assertThat(addField.getBoolean(TurSNSiteFieldExtAPI.MULTI_VALUED)).isTrue();
                assertThat(addField.getString(TurSNSiteFieldExtAPI.TYPE))
                                .isEqualTo(TurSNSiteFieldExtAPI.STRING);
        }

        @Test
        void testResolveSolrTypeUsesPdateForDate() throws Exception {
                TurSNSiteFieldExtAPI api = new TurSNSiteFieldExtAPI(mock(TurSNSiteRepository.class),
                                mock(TurSNSiteFieldExtRepository.class), mock(TurSNSiteFieldExtFacetRepository.class),
                                mock(TurSNSiteFieldRepository.class), mock(TurSNSiteLocaleRepository.class),
                                mock(TurSEInstanceRepository.class), mock(TurSNTemplate.class));
                TurSNSiteFieldExt fieldExt = new TurSNSiteFieldExt();
                fieldExt.setType(TurSEFieldType.DATE);

                String type = (String) invokePrivate(api, "resolveSolrType",
                                new Class<?>[] { TurSNSiteFieldExt.class, boolean.class }, fieldExt, false);

                assertThat(type).isEqualTo(TurSNSiteFieldExtAPI.PDATE);
        }

        @Test
        void testGetSolrFieldNamePrefixesNer() throws Exception {
                TurSNSiteFieldExtAPI api = new TurSNSiteFieldExtAPI(mock(TurSNSiteRepository.class),
                                mock(TurSNSiteFieldExtRepository.class), mock(TurSNSiteFieldExtFacetRepository.class),
                                mock(TurSNSiteFieldRepository.class), mock(TurSNSiteLocaleRepository.class),
                                mock(TurSEInstanceRepository.class), mock(TurSNTemplate.class));
                TurSNSiteFieldExt fieldExt = new TurSNSiteFieldExt();
                fieldExt.setSnType(TurSNFieldType.NER);
                fieldExt.setName("person");

                String fieldName = (String) invokePrivate(api, "getSolrFieldName",
                                new Class<?>[] { TurSNSiteFieldExt.class }, fieldExt);

                assertThat(fieldName).isEqualTo("turing_entity_person");
        }

        @Test
        void testUpdateSolrSchemaSkipsWhenInstanceMissing() throws Exception {
                TurSEInstanceRepository instanceRepository = mock(TurSEInstanceRepository.class);
                TurSNSiteFieldExtAPI api = new TurSNSiteFieldExtAPI(mock(TurSNSiteRepository.class),
                                mock(TurSNSiteFieldExtRepository.class), mock(TurSNSiteFieldExtFacetRepository.class),
                                mock(TurSNSiteFieldRepository.class), mock(TurSNSiteLocaleRepository.class),
                                instanceRepository, mock(TurSNTemplate.class));
                TurSNSite site = new TurSNSite();
                site.setTurSNSiteLocales(new HashSet<>());

                invokePrivate(api, "updateSolrSchema",
                                new Class<?>[] { TurSNSite.class, TurSNSiteField.class }, site, new TurSNSiteField());

                verify(instanceRepository, never()).findById(ArgumentMatchers.anyString());
        }

        @Test
        void testDeleteSolrSchemaSkipsWhenLocalesEmpty() throws Exception {
                TurSEInstanceRepository instanceRepository = mock(TurSEInstanceRepository.class);
                TurSNSiteFieldExtAPI api = new TurSNSiteFieldExtAPI(mock(TurSNSiteRepository.class),
                                mock(TurSNSiteFieldExtRepository.class), mock(TurSNSiteFieldExtFacetRepository.class),
                                mock(TurSNSiteFieldRepository.class), mock(TurSNSiteLocaleRepository.class),
                                instanceRepository, mock(TurSNTemplate.class));
                TurSNSite site = new TurSNSite();
                TurSEInstance instance = new TurSEInstance();
                instance.setId("se-id");
                site.setTurSEInstance(instance);
                site.setTurSNSiteLocales(new HashSet<>());

                invokePrivate(api, "deleteSolrSchema",
                                new Class<?>[] { TurSNSite.class, TurSNSiteField.class }, site, new TurSNSiteField());

                verify(instanceRepository, never()).findById("se-id");
        }
}
