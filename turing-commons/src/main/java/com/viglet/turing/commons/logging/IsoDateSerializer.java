package com.viglet.turing.commons.logging;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

public class IsoDateSerializer extends StdSerializer<Date> {

    // No v3, o construtor padrão chamando super() é fortemente recomendado
    public IsoDateSerializer() {
        super(Date.class);
    }

    @Override
    public void serialize(Date date, JsonGenerator jsonGenerator, SerializationContext provider)
            throws JacksonException {
        if (date == null) {
            jsonGenerator.writeNull();
            return;
        }

        ZonedDateTime zdt = date.toInstant().atZone(ZoneId.systemDefault());
        String isoOffsetString = zdt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        jsonGenerator.writeRawValue("ISODate(\"" + isoOffsetString + "\")");
    }
}