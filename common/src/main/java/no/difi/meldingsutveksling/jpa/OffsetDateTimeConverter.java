package no.difi.meldingsutveksling.jpa;

import no.difi.meldingsutveksling.DateTimeUtil;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.sql.Timestamp;
import java.time.OffsetDateTime;

@Converter(autoApply = true)
public class OffsetDateTimeConverter implements AttributeConverter<OffsetDateTime, Timestamp> {

    @Override
    public Timestamp convertToDatabaseColumn(OffsetDateTime offsetDateTime) {
        return offsetDateTime != null ? Timestamp.from(offsetDateTime.toInstant()) : null;
    }

    @Override
    public OffsetDateTime convertToEntityAttribute(Timestamp timestamp) {
        return timestamp != null ? OffsetDateTime.ofInstant(timestamp.toInstant(), DateTimeUtil.DEFAULT_ZONE_ID) : null;
    }
}
