package com.viglet.turing.spring.logging;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class IsoDateSerializer extends JsonSerializer<Date> {
    @Override
    public void serialize(Date date, JsonGenerator jsonGenerator, SerializerProvider provider) throws IOException {
        ZonedDateTime zdt = date.toInstant().atZone(ZoneId.systemDefault());
        String isoOffsetString = zdt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        jsonGenerator.writeRaw(": ISODate(\"" + isoOffsetString + "\")");
    }
}
