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

package com.viglet.turing.connector.aem.commons.deserializer;

import java.io.IOException;
import java.util.Date;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TurAemUnixTimestamp extends StdDeserializer<Date> {

    public TurAemUnixTimestamp() {
        this(null);
    }

    public TurAemUnixTimestamp(Class<?> vc) {
        super(vc);
    }

    @Override
    public Date deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
            throws IOException {
        String timestamp = jsonParser.getText().trim();
        if (timestamp.isEmpty()) {
            return null;
        }
        try {
            // Pad to at least 10 digits (seconds), but not arbitrarily to 12
            while (timestamp.length() < 10) {
                timestamp += "0";
            }
            long timeMillis;
            if (timestamp.length() == 10) {
                // Assume seconds, convert to milliseconds
                timeMillis = Long.parseLong(timestamp) * 1000L;
            } else {
                // Assume milliseconds
                timeMillis = Long.parseLong(timestamp);
            }
            Date date = new Date(timeMillis);
            return date;
        } catch (NumberFormatException e) {
            log.error("Unable to deserialize timestamp: {}", timestamp, e);
            return null;
        }
    }
}
