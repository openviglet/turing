/*
 *
 * Copyright (C) 2016-2024 the original author or authors.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.viglet.turing.connector.plugin.aem.persistence.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.viglet.turing.spring.jpa.TurUuid;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
@Table(name = "aem_source")
public class TurAemSource implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @TurUuid
    private String id;
    @Column
    private String name;
    @Column
    private String endpoint;
    @Column
    private String username;
    @Column
    private String password;
    @Column
    private String rootPath;
    @Column
    private String contentType;
    @Column
    private String subType;
    @Column
    private String oncePattern;
    @Column
    private Locale defaultLocale;
    @Column
    private String localeClass;
    @Column
    private String deltaClass;
    @Column
    private boolean author;
    @Column
    private boolean publish;
    @Column
    private String authorSNSite;
    @Column
    private String publishSNSite;
    @Column
    private String authorURLPrefix;
    @Column
    private String publishURLPrefix;

    @Builder.Default
    @OneToMany(mappedBy = "turAemSource", orphanRemoval = true, fetch = FetchType.LAZY)
    @Cascade({ org.hibernate.annotations.CascadeType.ALL, org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Collection<TurAemSourceLocalePath> localePaths = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "turAemSource", orphanRemoval = true, fetch = FetchType.LAZY)
    @Cascade({ org.hibernate.annotations.CascadeType.ALL, org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Collection<TurAemAttributeSpecification> attributeSpecifications = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "turAemSource", orphanRemoval = true, fetch = FetchType.LAZY)
    @Cascade({ org.hibernate.annotations.CascadeType.ALL, org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Collection<TurAemPluginModel> models = new HashSet<>();
}
