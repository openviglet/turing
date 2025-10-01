/*
 *
 * Copyright (C) 2016-2024 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If
 * not, see <https://www.gnu.org/licenses/>.
 */

package com.viglet.turing.connector.persistence.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import com.viglet.turing.commons.indexing.TurIndexingStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@NamedEntityGraph(name = "TurConnectorIndexingModel.dependencies",
        attributeNodes = @NamedAttributeNode("dependencies"))
@Builder
@RequiredArgsConstructor
@Accessors(chain = true)
@Setter
@Getter
@Entity
@Table(name = "con_indexing", uniqueConstraints = {@UniqueConstraint(columnNames = {"id"})})
@AllArgsConstructor
public class TurConnectorIndexingModel implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    @Version
    private Integer version;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, unique = true, length = 11)
    private int id;
    @Column(length = 500)
    private String objectId;
    @Column
    private String provider;
    @Column
    private String source;
    @Column
    private String environment;
    @Column
    private String transactionId;
    @Column
    private String checksum;
    @Column
    private Locale locale;
    @Column
    private Date created;
    @Column
    private Date modificationDate;
    @Column
    private boolean standalone;
    @Enumerated(EnumType.STRING)
    @Column
    private TurIndexingStatus status;
    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "con_indexing_sites", joinColumns = @JoinColumn(name = "indexing_id"))
    private List<String> sites = new ArrayList<>();
    @Builder.Default
    @OneToMany(mappedBy = "reference", orphanRemoval = true, fetch = FetchType.LAZY)
    @Cascade({CascadeType.ALL, CascadeType.DELETE_ORPHAN})
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Set<TurConnectorDependencyModel> dependencies = new HashSet<>();

    public void setDependencies(Set<TurConnectorDependencyModel> dependencyModels) {
        this.dependencies.clear();
        if (dependencyModels != null) {
            this.dependencies.addAll(dependencyModels);
        }
    }
}
