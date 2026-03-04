package com.viglet.turing.api.sn.console;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.persistence.model.sn.merge.TurSNSiteMergeProviders;
import com.viglet.turing.persistence.model.sn.merge.TurSNSiteMergeProvidersField;
import com.viglet.turing.persistence.repository.sn.TurSNSiteRepository;
import com.viglet.turing.persistence.repository.sn.merge.TurSNSiteMergeProvidersFieldRepository;
import com.viglet.turing.persistence.repository.sn.merge.TurSNSiteMergeProvidersRepository;

@ExtendWith(MockitoExtension.class)
class TurSNSiteMergeProvidersAPITest {

    @Mock
    private TurSNSiteRepository turSNSiteRepository;
    @Mock
    private TurSNSiteMergeProvidersRepository turSNSiteMergeRepository;
    @Mock
    private TurSNSiteMergeProvidersFieldRepository turSNSiteMergeFieldRepository;

    private TurSNSiteMergeProvidersAPI api;

    @BeforeEach
    void setUp() {
        api = new TurSNSiteMergeProvidersAPI(turSNSiteRepository, turSNSiteMergeRepository,
                turSNSiteMergeFieldRepository);
    }

    @Test
    void shouldListMergeProvidersWhenSiteExists() {
        TurSNSite site = new TurSNSite();
        TurSNSiteMergeProviders provider = new TurSNSiteMergeProviders();
        when(turSNSiteRepository.findById("site-1")).thenReturn(Optional.of(site));
        when(turSNSiteMergeRepository.findByTurSNSite(site)).thenReturn(List.of(provider));

        List<TurSNSiteMergeProviders> result = api.turSNSiteMergeList("site-1");

        assertThat(result).containsExactly(provider);
    }

    @Test
    void shouldReturnEmptyListWhenSiteDoesNotExist() {
        when(turSNSiteRepository.findById("missing")).thenReturn(Optional.empty());

        List<TurSNSiteMergeProviders> result = api.turSNSiteMergeList("missing");

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnMergeProviderWithOverwrittenFields() {
        TurSNSiteMergeProviders mergeProviders = new TurSNSiteMergeProviders();
        TurSNSiteMergeProvidersField overwrittenField = new TurSNSiteMergeProvidersField();
        when(turSNSiteMergeRepository.findById("merge-1")).thenReturn(Optional.of(mergeProviders));
        when(turSNSiteMergeFieldRepository.findByTurSNSiteMergeProviders(mergeProviders))
                .thenReturn(Set.of(overwrittenField));

        TurSNSiteMergeProviders result = api.turSNSiteFieldExtGet("ignored", "merge-1");

        assertThat(result).isSameAs(mergeProviders);
        assertThat(result.getOverwrittenFields()).containsExactly(overwrittenField);
    }

    @Test
    void shouldReturnNewMergeProviderWhenIdNotFound() {
        when(turSNSiteMergeRepository.findById("missing")).thenReturn(Optional.empty());

        TurSNSiteMergeProviders result = api.turSNSiteFieldExtGet("ignored", "missing");

        assertThat(result).isNotNull();
        assertThat(result.getId()).isNull();
    }

    @Test
    void shouldUpdateMergeProviderAndPersistOverwrittenFields() {
        TurSNSite site = new TurSNSite();
        TurSNSiteMergeProviders existing = new TurSNSiteMergeProviders();
        existing.setOverwrittenFields(new java.util.HashSet<>(Set.of(new TurSNSiteMergeProvidersField())));

        TurSNSiteMergeProvidersField firstField = new TurSNSiteMergeProvidersField();
        TurSNSiteMergeProvidersField secondField = new TurSNSiteMergeProvidersField();

        TurSNSiteMergeProviders request = new TurSNSiteMergeProviders();
        request.setProviderFrom("provider-a");
        request.setProviderTo("provider-b");
        request.setRelationFrom("from");
        request.setRelationTo("to");
        request.setDescription("updated");
        request.setLocale(Locale.CANADA);
        request.setTurSNSite(site);
        request.setOverwrittenFields(Set.of(firstField, secondField));

        when(turSNSiteMergeRepository.findById("merge-1")).thenReturn(Optional.of(existing));

        TurSNSiteMergeProviders result = api.turSNSiteMergeUpdate("merge-1", request, "ignored");

        assertThat(result).isSameAs(existing);
        assertThat(result.getProviderFrom()).isEqualTo("provider-a");
        assertThat(result.getProviderTo()).isEqualTo("provider-b");
        assertThat(result.getRelationFrom()).isEqualTo("from");
        assertThat(result.getRelationTo()).isEqualTo("to");
        assertThat(result.getDescription()).isEqualTo("updated");
        assertThat(result.getLocale()).isEqualTo(Locale.CANADA);
        assertThat(result.getTurSNSite()).isSameAs(site);

        verify(turSNSiteMergeRepository).save(existing);
        verify(turSNSiteMergeFieldRepository).save(firstField);
        verify(turSNSiteMergeFieldRepository).save(secondField);
        assertThat(firstField.getTurSNSiteMergeProviders()).isSameAs(existing);
        assertThat(secondField.getTurSNSiteMergeProviders()).isSameAs(existing);
    }

    @Test
    void shouldReturnNewMergeProviderWhenUpdateIdNotFound() {
        when(turSNSiteMergeRepository.findById("missing")).thenReturn(Optional.empty());

        TurSNSiteMergeProviders result = api.turSNSiteMergeUpdate("missing", new TurSNSiteMergeProviders(), "ignored");

        assertThat(result.getId()).isNull();
    }

    @Test
    void shouldDeleteMergeProviderById() {
        boolean deleted = api.turSNSiteMergeDelete("merge-1", "ignored");

        assertThat(deleted).isTrue();
        verify(turSNSiteMergeRepository).deleteById("merge-1");
    }

    @Test
    void shouldAddMergeProviderAndPersistFields() {
        TurSNSiteMergeProviders merge = new TurSNSiteMergeProviders();
        TurSNSiteMergeProvidersField field = new TurSNSiteMergeProvidersField();
        merge.setOverwrittenFields(Set.of(field));

        TurSNSiteMergeProviders result = api.turSNSiteMergeAdd(merge, "ignored");

        assertThat(result).isSameAs(merge);
        verify(turSNSiteMergeRepository).save(merge);
        verify(turSNSiteMergeFieldRepository).save(field);
        assertThat(field.getTurSNSiteMergeProviders()).isSameAs(merge);
    }

    @Test
    void shouldCreateMergeStructureWithDefaultLocaleWhenSiteExists() {
        TurSNSite site = new TurSNSite();
        when(turSNSiteRepository.findById("site-1")).thenReturn(Optional.of(site));

        TurSNSiteMergeProviders structure = api.turSNSiteMergeStructure("site-1");

        assertThat(structure.getLocale()).isEqualTo(Locale.US);
        assertThat(structure.getTurSNSite()).isSameAs(site);
    }

    @Test
    void shouldReturnEmptyMergeStructureWhenSiteNotFound() {
        when(turSNSiteRepository.findById("missing")).thenReturn(Optional.empty());

        TurSNSiteMergeProviders structure = api.turSNSiteMergeStructure("missing");

        assertThat(structure.getId()).isNull();
        assertThat(structure.getTurSNSite()).isNull();
        assertThat(structure.getOverwrittenFields()).isNotNull();
    }
}
