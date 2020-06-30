/*
 * Copyright (C) 2016-2019 the original author or authors. 
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.viglet.turing.plugins.otca.af.xml;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for AFConfigExtractionTypeEnum.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="AFConfigExtractionTypeEnum">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="dynamic"/>
 *     &lt;enumeration value="entity"/>
 *     &lt;enumeration value="candidate"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "AFConfigExtractionTypeEnum")
@XmlEnum
public enum AFConfigExtractionTypeEnum {

    @XmlEnumValue("dynamic")
    DYNAMIC("dynamic"),
    @XmlEnumValue("entity")
    ENTITY("entity"),
    @XmlEnumValue("candidate")
    CANDIDATE("candidate");
    private final String value;

    AFConfigExtractionTypeEnum(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static AFConfigExtractionTypeEnum fromValue(String v) {
        for (AFConfigExtractionTypeEnum c: AFConfigExtractionTypeEnum.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
