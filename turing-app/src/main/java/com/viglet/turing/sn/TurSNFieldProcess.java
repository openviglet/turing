package com.viglet.turing.sn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.viglet.turing.persistence.model.sn.field.TurSNSiteCustomFacet;
import com.viglet.turing.persistence.model.sn.field.TurSNSiteFieldExt;
import com.viglet.turing.persistence.repository.sn.TurSNSiteRepository;
import com.viglet.turing.persistence.repository.sn.field.TurSNSiteFieldExtRepository;

/**
 * @author Alexandre Oliveira
 * @since 0.3.9
 */
@Component
public class TurSNFieldProcess {
    private final TurSNSiteRepository turSNSiteRepository;
    private final TurSNSiteFieldExtRepository turSNSiteFieldExtRepository;

    public TurSNFieldProcess(TurSNSiteRepository turSNSiteRepository,
            TurSNSiteFieldExtRepository turSNSiteFieldExtRepository) {
        this.turSNSiteRepository = turSNSiteRepository;
        this.turSNSiteFieldExtRepository = turSNSiteFieldExtRepository;
    }

    @NotNull
    public Optional<List<TurSNSiteFieldExt>> getTurSNSiteFieldOrdering(String snSiteId) {
        return turSNSiteRepository.findById(snSiteId)
                .map(turSNSite -> {
                    List<TurSNSiteFieldExt> enabledFields = turSNSiteFieldExtRepository
                            .findByTurSNSiteAndEnabled(turSNSite, 1);

                    List<TurSNSiteFieldExt> facetFields = enabledFields.stream()
                            .filter(field -> field.getFacet() == 1)
                            .toList();

                    List<TurSNSiteFieldExt> customFacetEntries = enabledFields.stream()
                            .flatMap(field -> Optional.ofNullable(field.getCustomFacets())
                                    .orElse(Collections.emptySet())
                                    .stream()
                                    .map(customFacet -> getCustomFacetEntry(field, customFacet)))
                            .toList();

                    List<TurSNSiteFieldExt> facetEntries = new ArrayList<>(facetFields);
                    facetEntries.addAll(customFacetEntries);
                    return facetEntries.stream()
                            .sorted(Comparator.comparing(this::getFacetPosition)
                                    .thenComparing(TurSNSiteFieldExt::getFacetName,
                                            Comparator.nullsLast(String::compareToIgnoreCase)))
                            .toList();
                });
    }

    private TurSNSiteFieldExt getCustomFacetEntry(TurSNSiteFieldExt field,
            TurSNSiteCustomFacet customFacet) {
        TurSNSiteFieldExt customFacetEntry = new TurSNSiteFieldExt();
        customFacetEntry.setId(customFacet.getId());
        customFacetEntry.setName(customFacet.getName());
        customFacetEntry.setFacetName(getCustomFacetLabel(customFacet));
        customFacetEntry.setFacetPosition(getCustomFacetPosition(customFacet, field));
        customFacetEntry.setFacet(1);
        customFacetEntry.setEnabled(1);
        customFacetEntry.setTurSNSite(field.getTurSNSite());
        customFacetEntry.setType(field.getType());
        customFacetEntry.setSnType(field.getSnType());
        customFacetEntry.setSecondaryFacet(field.getSecondaryFacet());
        customFacetEntry.setShowAllFacetItems(field.getShowAllFacetItems());
        return customFacetEntry;
    }

    private String getCustomFacetLabel(TurSNSiteCustomFacet customFacet) {
        return Optional.ofNullable(customFacet.getLabel())
                .map(labels -> labels.values().stream().findFirst().orElse(customFacet.getName()))
                .orElse(customFacet.getName());
    }

    private Integer getFacetPosition(TurSNSiteFieldExt fieldExtension) {
        return Optional.ofNullable(fieldExtension.getFacetPosition())
                .filter(position -> position > 0)
                .orElse(Integer.MAX_VALUE);
    }

    private Integer getCustomFacetPosition(TurSNSiteCustomFacet customFacet,
            TurSNSiteFieldExt fieldExtension) {
        return Optional.ofNullable(customFacet.getFacetPosition())
                .filter(position -> position > 0)
                .orElseGet(() -> getFacetPosition(fieldExtension));
    }
}
