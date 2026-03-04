package com.viglet.turing.persistence.model.sn.facet;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "sn_site_custom_facet_parent")
public class TurSNSiteCustomFacetParentModel implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private String id;

    @Column(name = "idName", nullable = false, unique = true)
    private String idName;

    @Column
    private String attribute;

    @Column
    private String selection;

    @OneToMany(mappedBy = "parent")
    private List<TurSNSiteCustomFacetModel> items;
}
