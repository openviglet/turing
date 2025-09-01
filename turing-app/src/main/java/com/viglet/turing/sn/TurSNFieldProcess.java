package com.viglet.turing.sn;

import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

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
                .map(turSNSite -> turSNSiteFieldExtRepository
                        .findByTurSNSiteAndFacetAndEnabledOrderByFacetPosition(turSNSite, 1, 1));
    }
}
