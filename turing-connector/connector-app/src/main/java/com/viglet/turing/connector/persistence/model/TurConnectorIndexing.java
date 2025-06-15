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

package com.viglet.turing.connector.persistence.model;

import com.viglet.turing.commons.indexing.TurIndexingStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;

@Builder
@RequiredArgsConstructor
@Accessors(chain = true)
@Setter
@Getter
@Entity
@Table(name = "con_indexing", uniqueConstraints = {@UniqueConstraint(columnNames = {"id"})})
@AllArgsConstructor
public class TurConnectorIndexing implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, unique = true, length = 11)
    private int id;
    @Column(length = 500)
    private String objectId;
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

    @Enumerated(EnumType.STRING)
    @Column
    private TurIndexingStatus status;

    @Builder.Default
    @Convert(converter = TurStringListConverter.class)
    private List<String> sites = new ArrayList<>();
}
