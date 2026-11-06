package com.viglet.turing.exchange.sn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.persistence.model.sn.synonym.TurSNSynonym;
import com.viglet.turing.persistence.model.sn.synonym.TurSNSynonymType;
import com.viglet.turing.persistence.repository.sn.synonym.TurSNSynonymRepository;
import com.viglet.turing.sn.synonym.TurSNSynonymApplyService;

@ExtendWith(MockitoExtension.class)
class TurSNSiteSynonymExchangeServiceTest {

    @Mock
    private TurSNSynonymRepository synonymRepository;
    @Mock
    private TurSNSynonymApplyService applyService;

    @InjectMocks
    private TurSNSiteSynonymExchangeService service;

    private static TurSNSite site() {
        TurSNSite site = new TurSNSite();
        site.setId("tarnfell");
        site.setName("tarnfell");
        return site;
    }

    @Test
    void exportCarriesEveryFieldOfARule() {
        TurSNSite site = site();
        TurSNSynonym synonym = new TurSNSynonym();
        synonym.setId("syn-1");
        synonym.setName("rucksack");
        synonym.setType(TurSNSynonymType.ONE_WAY);
        synonym.setLanguage(Locale.US);
        synonym.setInput("rucksack");
        synonym.setTerms(List.of("backpack", "daypack"));
        synonym.setEnabled(false);
        when(synonymRepository.findByTurSNSite(any(Sort.class), any(TurSNSite.class)))
                .thenReturn(List.of(synonym));

        assertThat(service.export(site)).containsExactly(new TurSNSynonymExchange("syn-1", "rucksack",
                TurSNSynonymType.ONE_WAY, Locale.US, "rucksack", List.of("backpack", "daypack"), false));
    }

    @Test
    void snapshotDropsTheIdsSoTheRulesAreWrittenAsNew() {
        TurSNSynonym synonym = new TurSNSynonym();
        synonym.setId("syn-1");
        synonym.setLanguage(Locale.US);
        synonym.setTerms(List.of("puffer", "down jacket"));
        when(synonymRepository.findByTurSNSite(any(Sort.class), any(TurSNSite.class)))
                .thenReturn(List.of(synonym));

        assertThat(service.snapshot(site())).singleElement()
                .satisfies(s -> {
                    assertThat(s.id()).isNull();
                    assertThat(s.terms()).containsExactly("puffer", "down jacket");
                });
    }

    @Test
    void restoreSavesTheRulesOnTheSiteAndAppliesThem() {
        TurSNSite site = site();
        when(synonymRepository.existsById("syn-1")).thenReturn(false);
        when(applyService.applyAll("tarnfell")).thenReturn(Map.of());

        int saved = service.restore(site, List.of(
                new TurSNSynonymExchange("syn-1", null, null, Locale.US, null,
                        List.of("raincoat", "rain jacket"), true),
                new TurSNSynonymExchange("syn-2", null, null, null, null, List.of("x"), true)));

        assertThat(saved).isEqualTo(1);
        ArgumentCaptor<TurSNSynonym> captor = ArgumentCaptor.forClass(TurSNSynonym.class);
        verify(synonymRepository).save(captor.capture());
        TurSNSynonym written = captor.getValue();
        assertThat(written.getId()).isEqualTo("syn-1");
        assertThat(written.getTurSNSite()).isSameAs(site);
        assertThat(written.getType()).isEqualTo(TurSNSynonymType.REGULAR);
        assertThat(written.getTerms()).containsExactly("raincoat", "rain jacket");
        verify(applyService, times(1)).applyAll("tarnfell");
    }

    @Test
    void restoreMintsANewIdWhenAnotherRowHoldsTheExportedOne() {
        when(synonymRepository.existsById("syn-1")).thenReturn(true);
        when(applyService.applyAll("tarnfell")).thenReturn(Map.of());

        service.restore(site(), List.of(new TurSNSynonymExchange("syn-1", null,
                TurSNSynonymType.REGULAR, Locale.US, null, List.of("a", "b"), true)));

        ArgumentCaptor<TurSNSynonym> captor = ArgumentCaptor.forClass(TurSNSynonym.class);
        verify(synonymRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isNull();
    }

    @Test
    void restoreOfNothingNeitherSavesNorApplies() {
        assertThat(service.restore(site(), null)).isZero();
        assertThat(service.restore(site(), List.of())).isZero();
        verify(synonymRepository, never()).save(any());
        verify(applyService, never()).applyAll(any());
    }

    @Test
    void anApplyFailureDoesNotFailTheImport() {
        when(synonymRepository.existsById("syn-1")).thenReturn(false);
        when(applyService.applyAll("tarnfell")).thenThrow(new IllegalStateException("engine down"));

        assertThat(service.restore(site(), List.of(new TurSNSynonymExchange("syn-1", null,
                TurSNSynonymType.REGULAR, Locale.US, null, List.of("a", "b"), true)))).isEqualTo(1);
    }
}
