package no.difi.meldingsutveksling.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import no.difi.meldingsutveksling.jackson.PartnerIdentifierModule;
import no.difi.meldingsutveksling.jackson.StandardBusinessDocumentModule;
import no.difi.meldingsutveksling.jpa.ObjectMapperHolder;
import no.difi.meldingsutveksling.nextmove.BusinessMessageType;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.ext.javatime.deser.InstantDeserializer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQueries;

import static no.difi.meldingsutveksling.DateTimeUtil.DEFAULT_ZONE_ID;

@Configuration
public class JacksonConfig {

    @Bean
    public JsonMapperBuilderCustomizer jacksonCustomizer(Clock clock) {

        return builder ->
            builder.addModules(new StandardBusinessDocumentModule(BusinessMessageType::fromType), new PartnerIdentifierModule())
                .addModule(new SimpleModule("IsoDateTimeModule")
                    .addDeserializer(OffsetDateTime.class, new IsoDateTimeDeserializer(clock)))
                .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_NULL))
                // Jackson 3 auto-detects multi-arg constructors via parameter names, which
                // bypasses no-arg constructors that set default values (e.g. ServiceRecord,
                // whose postAddress then ends up null instead of PostAddress.EMPTY).
                // Creator visibility NONE restores the Jackson 2 behaviour: implicit creators
                // are not used, only @JsonCreator-annotated ones. Records are unaffected
                // (canonical record constructors are resolved through a separate path).
                .changeDefaultVisibility(vc -> vc.withCreatorVisibility(JsonAutoDetect.Visibility.NONE))
                .enable(SerializationFeature.INDENT_OUTPUT)
                .enable(EnumFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
                .enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS)
                .enable(MapperFeature.DEFAULT_VIEW_INCLUSION)
                .disable(SerializationFeature.CLOSE_CLOSEABLE)
                .disable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY,
                    DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS,
                    DateTimeFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);
    }

    @SuppressWarnings("squid:MaximumInheritanceDepth")
    private static final class IsoDateTimeDeserializer extends InstantDeserializer<OffsetDateTime> {

        IsoDateTimeDeserializer(Clock clock) {
            super(
                OffsetDateTime.class,
                DateTimeFormatter.ISO_DATE_TIME,
                temporal -> getOffsetDateTime(clock, temporal),
                a -> OffsetDateTime.ofInstant(Instant.ofEpochMilli(a.value), a.zoneId),
                a -> OffsetDateTime.ofInstant(Instant.ofEpochSecond(a.integer, a.fraction), a.zoneId),
                (d, z) -> d.withOffsetSameInstant(z.getRules().getOffset(d.toLocalDateTime())),
                true, // yes, replace +0000 with Z
                true, // normalizeZoneId
                false // readNumericStringsAsTimestamp
            );
        }

        private static OffsetDateTime getOffsetDateTime(Clock clock, TemporalAccessor temporal) {
            ZoneId obj = temporal.query(TemporalQueries.zone());

            if (obj != null) {
                return OffsetDateTime.from(temporal);
            }

            return LocalDateTime.from(temporal)
                .atOffset(DEFAULT_ZONE_ID.getRules().getOffset(LocalDateTime.now(clock)));
        }
    }

    @Bean
    public ObjectMapperHolder objectMapperHolder(JsonMapper jsonMapper) {
        return new ObjectMapperHolder(jsonMapper);
    }
}
