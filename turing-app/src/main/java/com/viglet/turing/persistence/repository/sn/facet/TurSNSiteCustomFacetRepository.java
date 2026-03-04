package com.viglet.turing.persistence.repository.sn.facet;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.viglet.turing.persistence.model.sn.facet.TurSNSiteCustomFacetModel;

public interface TurSNSiteCustomFacetRepository extends JpaRepository<TurSNSiteCustomFacetModel, String> {

    boolean existsByLabel(String label);

    Optional<TurSNSiteCustomFacetModel> findByLabel(String label);
}
