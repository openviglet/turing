package com.viglet.turing.persistence.model.sn.facet;

import java.io.Serial;
import java.io.Serializable;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "sn_site_custom_facet")
public class TurSNSiteCustomFacetModel implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private String id;

    @Column
    private String label;

    @Column
    private String rangeStart;

    @Column
    private String rangeEnd;

    @ManyToOne
    @JoinColumn(name = "parent_id")
    private TurSNSiteCustomFacetParentModel parent;
}
