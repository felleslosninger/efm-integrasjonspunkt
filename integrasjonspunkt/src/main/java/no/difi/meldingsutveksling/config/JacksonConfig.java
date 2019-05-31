package no.difi.meldingsutveksling.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.InstantDeserializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalQueries;

import static no.difi.meldingsutveksling.DateTimeUtil.DEFAULT_ZONE_ID;

@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer(Clock clock) {

        return builder ->
                builder.modulesToInstall(new JavaTimeModule())
                        .deserializerByType(OffsetDateTime.class, new IsoDateTimeDeserializer(clock))
                        .serializationInclusion(JsonInclude.Include.NON_NULL)
                        .featuresToEnable(SerializationFeature.INDENT_OUTPUT)
                        .featuresToDisable(
                                SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,
                                SerializationFeature.CLOSE_CLOSEABLE,
                                DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY,
                                DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                                DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);
    }

    private static final class IsoDateTimeDeserializer extends InstantDeserializer<OffsetDateTime> {

        IsoDateTimeDeserializer(Clock clock) {
            super(
                    OffsetDateTime.class, DateTimeFormatter.ISO_DATE_TIME,
                    temporal -> {
                        ZoneId obj = temporal.query(TemporalQueries.zone());

                        if (obj != null) {
                            return OffsetDateTime.from(temporal);
                        }

                        return LocalDateTime.from(temporal)
                                .atOffset(DEFAULT_ZONE_ID.getRules().getOffset(LocalDateTime.now(clock)));
                    },
                    a -> OffsetDateTime.ofInstant(Instant.ofEpochMilli(a.value), a.zoneId),
                    a -> OffsetDateTime.ofInstant(Instant.ofEpochSecond(a.integer, a.fraction), a.zoneId),
                    (d, z) -> d.withOffsetSameInstant(z.getRules().getOffset(d.toLocalDateTime())),
                    true // yes, replace +0000 with Z
            );
        }
    }
}
