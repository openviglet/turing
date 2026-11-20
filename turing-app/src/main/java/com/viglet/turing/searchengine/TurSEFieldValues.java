/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.viglet.turing.searchengine;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

/**
 * Reads a stored field value as the one string a facet, a title or an id shows
 * (T1310, formerly {@code TurSolrField}). A multi-valued field gives its first value
 * and a date gives its UTC ISO instant, whichever engine stored it.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
public final class TurSEFieldValues {

    private static final String EMPTY_STRING = "";
    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    private static final String GMT = "GMT";

    private TurSEFieldValues() {
    }

    public static String toText(Object attrValue) {
        return switch (attrValue) {
            case null -> EMPTY_STRING;
            case String s -> s.trim();
            case ArrayList<?> arrayListValue -> arrayListToString(arrayListValue).trim();
            case Long longValue -> longValue.toString().trim();
            case Object[] objectValue -> objectArrayToString(objectValue).trim();
            case Date dateValue -> formatDate(dateValue);
            default -> attrValue.toString().trim();
        };
    }

    private static String objectArrayToString(Object[] arrAttrValue) {
        if (arrAttrValue[0] instanceof String stringValue) {
            return stringValue.trim();
        } else if (arrAttrValue[0] instanceof Long longValue) {
            return longValue.toString();
        } else if (arrAttrValue[0] instanceof Date dateValue) {
            return formatDate(dateValue);
        } else {
            return arrAttrValue[0].toString().trim();
        }
    }

    private static String formatDate(Date dateValue) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATE_FORMAT);
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone(GMT));
        return simpleDateFormat.format(dateValue);
    }

    private static String arrayListToString(ArrayList<?> arrAttValue) {
        if (arrAttValue.isEmpty()) {
            return EMPTY_STRING;
        }
        return switch (arrAttValue.getFirst()) {
            case null -> EMPTY_STRING;
            case String stringValue -> stringValue.trim();
            case Long longValue -> longValue.toString();
            case Date dateValue -> formatDate(dateValue);
            case Object firstElement -> firstElement.toString().trim();
        };
    }
}
