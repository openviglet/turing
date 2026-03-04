package com.viglet.turing.persistence.repository.sn.facet;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.viglet.turing.persistence.model.sn.facet.TurSNSiteCustomFacetParentModel;

public interface TurSNSiteCustomFacetParentRepository extends JpaRepository<TurSNSiteCustomFacetParentModel, String> {
    boolean existsByIdName(String idName);

    Optional<TurSNSiteCustomFacetParentModel> findByIdName(String idName);
}
