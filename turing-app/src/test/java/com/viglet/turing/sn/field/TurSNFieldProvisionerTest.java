package com.viglet.turing.sn.field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.viglet.turing.client.sn.job.TurSNJobAttributeSpec;
import com.viglet.turing.commons.se.field.TurSEFieldType;
import com.viglet.turing.persistence.model.se.TurSEInstance;
import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.persistence.model.sn.field.TurSNSiteField;
import com.viglet.turing.persistence.model.sn.field.TurSNSiteFieldExt;
import com.viglet.turing.persistence.model.sn.locale.TurSNSiteLocale;
import com.viglet.turing.persistence.repository.se.TurSEInstanceRepository;
import com.viglet.turing.persistence.repository.sn.field.TurSNSiteFieldExtFacetRepository;
import com.viglet.turing.persistence.repository.sn.field.TurSNSiteFieldExtRepository;
import com.viglet.turing.persistence.repository.sn.field.TurSNSiteFieldRepository;
import com.viglet.turing.persistence.repository.sn.locale.TurSNSiteLocaleRepository;
import com.viglet.turing.plugins.se.TurSearchEnginePlugin;
import com.viglet.turing.plugins.se.TurSearchEnginePluginFactory;

@ExtendWith(MockitoExtension.class)
class TurSNFieldProvisionerTest {

    @Mock
    private TurSNSiteFieldRepository turSNSiteFieldRepository;
    @Mock
    private TurSNSiteFieldExtRepository turSNSiteFieldExtRepository;
    @Mock
    private TurSNSiteFieldExtFacetRepository turSNSiteFieldExtFacetRepository;
    @Mock
    private com.viglet.turing.persistence.repository.sn.field.TurSNSiteCustomFacetRepository
            turSNSiteCustomFacetRepository;
    @Mock
    private TurSNSiteLocaleRepository turSNSiteLocaleRepository;
    @Mock
    private TurSEInstanceRepository turSEInstanceRepository;
    @Mock
    private TurSearchEnginePluginFactory pluginFactory;
    @InjectMocks
    private TurSNFieldProvisioner provisioner;

    private TurSNSite site() {
        TurSEInstance seInstance = new TurSEInstance();
        seInstance.setId("se1");
        TurSNSite site = new TurSNSite();
        site.setName("site1");
        site.setTurSEInstance(seInstance);
        return site;
    }

    @Test
    void ensureFieldCreatesSchemaAndSearchEngineFieldWhenAbsent() {
        TurSNSite site = site();
        TurSNSiteLocale locale = new TurSNSiteLocale();
        locale.setCore("core_en");
        locale.setTurSNSite(site);

        TurSNJobAttributeSpec spec = TurSNJobAttributeSpec.builder()
                .name("customField")
                .type(TurSEFieldType.STRING)
                .description("Custom")
                .multiValued(false)
                .mandatory(true)
                .facet(true)
                .facetName(Map.of("default", "Custom", "pt_BR", "Personalizado"))
                .build();

        TurSEInstance seInstance = new TurSEInstance();
        seInstance.setId("se1");
        TurSearchEnginePlugin plugin = org.mockito.Mockito.mock(TurSearchEnginePlugin.class);

        when(turSNSiteFieldExtRepository.existsByTurSNSiteAndName(site, "customField")).thenReturn(false);
        when(turSNSiteLocaleRepository.findByTurSNSite(site)).thenReturn(List.of(locale));
        when(turSEInstanceRepository.findById("se1")).thenReturn(Optional.of(seInstance));
        when(turSNSiteFieldRepository.save(any(TurSNSiteField.class))).thenAnswer(inv -> {
            TurSNSiteField f = inv.getArgument(0);
            f.setId("f1");
            return f;
        });
        when(turSNSiteFieldExtRepository.save(any(TurSNSiteFieldExt.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(pluginFactory.getPluginForSite(site)).thenReturn(plugin);
        when(plugin.fieldExists(seInstance, "core_en", "customField")).thenReturn(false);

        boolean created = provisioner.ensureField(site, spec);

        assertTrue(created);
        verify(turSNSiteFieldRepository).save(any(TurSNSiteField.class));
        ArgumentCaptor<TurSNSiteFieldExt> extCaptor = ArgumentCaptor.forClass(TurSNSiteFieldExt.class);
        verify(turSNSiteFieldExtRepository).save(extCaptor.capture());
        TurSNSiteFieldExt ext = extCaptor.getValue();
        // facet + mandatory→required carried from the spec; default facet label kept.
        assertEquals(1, ext.getFacet());
        assertEquals(1, ext.getRequired());
        assertEquals("Custom", ext.getFacetName());
        verify(turSNSiteFieldExtFacetRepository).saveAll(any());
        verify(plugin).addOrUpdateField(seInstance, "core_en",
                "customField", TurSEFieldType.STRING, true, false, true);
    }

    /**
     * T1241 — a migration deletes the ext's facet-locale and custom-facet rows
     * before the ext, so rows its collections never held cannot outlive it.
     */
    @Test
    void recreateFieldDeletesTheFacetRowsBeforeTheExt() {
        TurSNSite site = site();
        site.setTurSEInstance(null);
        TurSNSiteFieldExt ext = TurSNSiteFieldExt.builder().name("author").type(TurSEFieldType.STRING).build();
        var facet = new com.viglet.turing.persistence.model.sn.field.TurSNSiteFieldExtFacet();
        java.util.Set<com.viglet.turing.persistence.model.sn.field.TurSNSiteFieldExtFacet> facets =
                java.util.Set.of(facet);
        List<com.viglet.turing.persistence.model.sn.field.TurSNSiteCustomFacet> customFacets = List.of();
        TurSNJobAttributeSpec spec = TurSNJobAttributeSpec.builder()
                .name("author").type(TurSEFieldType.STRING).multiValued(true).build();

        when(turSNSiteFieldExtRepository.findByTurSNSite(any(), org.mockito.ArgumentMatchers.eq(site)))
                .thenReturn(List.of(ext));
        when(turSNSiteFieldExtFacetRepository.findByTurSNSiteFieldExt(ext)).thenReturn(facets);
        when(turSNSiteCustomFacetRepository.findByFieldExtWithDetails(ext)).thenReturn(customFacets);
        when(turSNSiteFieldRepository.findByTurSNSite(site)).thenReturn(List.of());
        when(turSNSiteFieldExtRepository.existsByTurSNSiteAndName(site, "author")).thenReturn(false);
        when(turSNSiteLocaleRepository.findByTurSNSite(site)).thenReturn(List.of());

        assertTrue(provisioner.recreateField(site, spec));

        var order = org.mockito.Mockito.inOrder(turSNSiteFieldExtFacetRepository, turSNSiteCustomFacetRepository,
                turSNSiteFieldExtRepository);
        order.verify(turSNSiteFieldExtFacetRepository).deleteAll(facets);
        order.verify(turSNSiteCustomFacetRepository).deleteAll(customFacets);
        order.verify(turSNSiteFieldExtRepository).delete(ext);
    }

    @Test
    void ensureFieldIsNoOpWhenFieldAlreadyDeclared() {
        TurSNSite site = site();
        TurSNJobAttributeSpec spec = TurSNJobAttributeSpec.builder()
                .name("existing").type(TurSEFieldType.STRING).build();

        when(turSNSiteFieldExtRepository.existsByTurSNSiteAndName(site, "existing")).thenReturn(true);

        boolean created = provisioner.ensureField(site, spec);

        assertFalse(created);
        verify(turSNSiteFieldRepository, never()).save(any());
        verify(turSNSiteFieldExtRepository, never()).save(any());
        verify(turSNSiteLocaleRepository, never()).findByTurSNSite(any());
    }
}
