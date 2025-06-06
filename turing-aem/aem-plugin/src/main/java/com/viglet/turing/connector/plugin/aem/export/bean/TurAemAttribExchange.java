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

package com.viglet.turing.connector.plugin.aem.export.bean;

import com.viglet.turing.commons.se.field.TurSEFieldType;
import lombok.*;

import java.util.HashMap;
import java.util.Map;

@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class TurAemAttribExchange {
    private String name;
    private String className;
    private String text;
    private TurSEFieldType type;
    private boolean mandatory;
    private boolean multiValued;
    private String description;
    private boolean facet;
    @Builder.Default
    private Map<String, String> facetName = new HashMap<>();
}
