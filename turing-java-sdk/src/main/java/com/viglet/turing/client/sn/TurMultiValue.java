/*
 * Copyright (C) 2016-2022 the original author or authors.
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
package com.viglet.turing.client.sn;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.TimeZone;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class TurMultiValue extends ArrayList<String> {

    private static final String FALSE = "false";
    private static final String TRUE = "true";
    private static final long serialVersionUID = 1L;
    public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    public static final String UTC = "UTC";
    private final boolean override;

    public TurMultiValue() {
        this.override = false;
    }

    public TurMultiValue(boolean override) {
        this.override = override;
    }

    public TurMultiValue(Collection<String> collection) {
        this(collection, false);
    }

    public TurMultiValue(Collection<String> collection, boolean override) {
        this.addAll(collection);
        this.override = override;
    }

    public static TurMultiValue fromDateCollection(Collection<Date> collection) {
        return fromDateCollection(collection, false);
    }

    public static TurMultiValue fromDateCollection(Collection<Date> collection, boolean override) {
        TurMultiValue turMultiValue = new TurMultiValue(override);
        if (collection != null) {
            for (Date date : collection) {
                turMultiValue.add(getDateFormat().format(date));
            }
        }
        return turMultiValue;
    }

    public static TurMultiValue fromBooleanCollection(Collection<Boolean> collection) {
        return fromBooleanCollection(collection, false);
    }

    public static TurMultiValue fromBooleanCollection(Collection<Boolean> collection,
            boolean override) {
        TurMultiValue turMultiValue = new TurMultiValue(override);
        if (collection != null) {
            for (Boolean bool : collection) {
                turMultiValue.add(getString(bool));
            }
        }
        return turMultiValue;
    }

    public static TurMultiValue fromIntegerCollection(Collection<Integer> collection) {
        return fromIntegerCollection(collection, false);
    }

    public static TurMultiValue fromIntegerCollection(Collection<Integer> collection,
            boolean override) {
        TurMultiValue turMultiValue = new TurMultiValue(override);
        if (collection != null) {
            for (Integer integer : collection) {
                turMultiValue.add(integer.toString());
            }
        }
        return turMultiValue;
    }

    public static TurMultiValue fromDoubleCollection(Collection<Double> collection) {
        return fromDoubleCollection(collection, false);
    }

    public static TurMultiValue fromDoubleCollection(Collection<Double> collection,
            boolean override) {
        TurMultiValue turMultiValue = new TurMultiValue(override);
        if (collection != null) {
            for (Double doubleValue : collection) {
                turMultiValue.add(doubleValue.toString());
            }
        }
        return turMultiValue;
    }

    public static TurMultiValue fromFloatCollection(Collection<Float> collection) {
        return fromFloatCollection(collection, false);
    }

    public static TurMultiValue fromFloatCollection(Collection<Float> collection,
            boolean override) {
        TurMultiValue turMultiValue = new TurMultiValue(override);
        if (collection != null) {
            for (Float floatValue : collection) {
                turMultiValue.add(floatValue.toString());
            }
        }
        return turMultiValue;
    }

    public static TurMultiValue fromLongCollection(Collection<Long> collection) {
        return fromLongCollection(collection, false);
    }

    public static TurMultiValue fromLongCollection(Collection<Long> collection, boolean override) {
        TurMultiValue turMultiValue = new TurMultiValue(override);
        if (collection != null) {
            for (Long longValue : collection) {
                turMultiValue.add(longValue.toString());
            }
        }
        return turMultiValue;
    }

    @Override
    public boolean equals(Object other) {
        return super.equals(other);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    public static TurMultiValue singleItem(String text) {
        return singleItem(text, false);
    }

    public static TurMultiValue singleItem(String text, boolean override) {
        TurMultiValue turMultiValue = new TurMultiValue(override);
        turMultiValue.add(text);
        return turMultiValue;
    }

    public static TurMultiValue singleItem(Boolean bool) {
        return singleItem(bool, false);
    }

    public static TurMultiValue singleItem(Boolean bool, boolean override) {
        TurMultiValue turMultiValue = new TurMultiValue(override);
        turMultiValue.add(getString(bool));
        return turMultiValue;
    }

    public static TurMultiValue singleItem(Integer integer) {
        return singleItem(integer, false);
    }

    public static TurMultiValue singleItem(Integer integer, boolean override) {
        TurMultiValue turMultiValue = new TurMultiValue(override);
        turMultiValue.add(integer.toString());
        return turMultiValue;
    }

    public static TurMultiValue singleItem(Double doubleValue) {
        return singleItem(doubleValue, false);
    }

    public static TurMultiValue singleItem(Double doubleValue, boolean override) {
        TurMultiValue turMultiValue = new TurMultiValue(override);
        turMultiValue.add(doubleValue.toString());
        return turMultiValue;
    }

    public static TurMultiValue singleItem(Float floatValue) {
        return singleItem(floatValue, false);
    }

    public static TurMultiValue singleItem(Float floatValue, boolean override) {
        TurMultiValue turMultiValue = new TurMultiValue(override);
        turMultiValue.add(floatValue.toString());
        return turMultiValue;
    }

    public static TurMultiValue singleItem(Long longValue) {
        return singleItem(longValue, false);
    }

    public static TurMultiValue singleItem(Long longValue, boolean override) {
        TurMultiValue turMultiValue = new TurMultiValue(override);
        turMultiValue.add(longValue.toString());
        return turMultiValue;
    }

    private static String getString(Boolean bool) {
        return bool.booleanValue() ? TRUE : FALSE;
    }

    public static TurMultiValue singleItem(boolean bool) {
        return singleItem(bool, false);
    }

    public static TurMultiValue singleItem(Date date) {
        return singleItem(date, false);
    }

    public static TurMultiValue singleItem(Date date, boolean override) {
        if (date == null) {
            return new TurMultiValue(override);
        } else {
            return singleItem(getDateFormat().format(date), override);
        }
    }

    private static DateFormat getDateFormat() {
        TimeZone tz = TimeZone.getTimeZone(UTC);
        DateFormat df = new SimpleDateFormat(DATE_FORMAT);
        df.setTimeZone(tz);
        return df;
    }

    public static TurMultiValue empty() {
        return new TurMultiValue();
    }
}
