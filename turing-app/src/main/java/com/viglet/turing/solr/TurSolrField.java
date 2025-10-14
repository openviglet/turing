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
package com.viglet.turing.solr;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

public class TurSolrField {

	public static final String EMPTY_STRING = "";
	public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
	public static final String GMT = "GMT";

	private TurSolrField() {
		throw new IllegalStateException("TurSolrField class");
	}

	public static String convertFieldToString(Object attrValue) {
		switch (attrValue) {
			case null -> {
				return EMPTY_STRING;
			}
			case String s -> {
				return s.trim();
			}
			case ArrayList<?> arrayListValue -> {
				return arrayListToString(arrayListValue).trim();
			}
			case Long longValue -> {
				return longToString(longValue).trim();
			}
			case Object[] objectValue -> {
				return objectArrayToString(objectValue).trim();
			}
			case Date dateValue -> {
				SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATE_FORMAT);
				simpleDateFormat.setTimeZone(TimeZone.getTimeZone(GMT));
				return simpleDateFormat.format(dateValue);
			}
			default -> {
				return attrValue.toString().trim();
			}
		}
	}

	private static String objectArrayToString(Object[] arrAttrValue) {
		if (arrAttrValue == null) {
			return EMPTY_STRING;
		} else if (arrAttrValue[0] instanceof String stringValue) {
			return stringValue.trim();
		} else if (arrAttrValue[0] instanceof Long longValue) {
			return longValue.toString();
		} else if (arrAttrValue[0] instanceof Date dateValue) {
			return dateValue.toString();
		} else {
			return arrAttrValue[0].toString().trim();
		}
	}

	private static String arrayListToString(ArrayList<?> arrAttValue) {
		if (arrAttValue == null || arrAttValue.isEmpty()) {
			return EMPTY_STRING;
		}
		Object firstElement = arrAttValue.get(0);
		if (firstElement == null) {
			return EMPTY_STRING;
		}
		if (firstElement instanceof String stringValue) {
			return stringValue.trim();
		} else if (firstElement instanceof Long longValue) {
			return longValue.toString();
		} else if (firstElement instanceof Date dateValue) {
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATE_FORMAT);
			simpleDateFormat.setTimeZone(TimeZone.getTimeZone(GMT));
			return simpleDateFormat.format(dateValue);
		} else {
			return firstElement.toString().trim();
		}
	}

	private static String longToString(Long longValue) {
		return longValue.toString();
	}
}
