package no.difi.meldingsutveksling.altinnv3.dpv;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;


public class AltinnTimestampDeserializerTest {

    ObjectMapper objectMapper = JsonMapper.builder()
        .addModule(new SimpleModule()
            .addDeserializer(OffsetDateTime.class, new AltinnOffsetDateTimeDeserializer()))
        .build();

    @ParameterizedTest
    @ValueSource(strings = {
        "2025-10-06T14:30:00+02:00",
        "2023-01-01T00:00:00Z",
        "2020-02-29T23:59:59-05:00",
    })
    public void handlesStandardValues(String date) {

        OffsetDateTime offsetDateTime = objectMapper.readValue("\"" + date + "\"", OffsetDateTime.class);

        var expected = OffsetDateTime.parse(date);

        assertEquals(expected, offsetDateTime, "Should deserialize offset date the same way as OffsetDateTime.parse");
    }

    @ParameterizedTest
    @CsvSource({
        "2025-10-06T14:30:00.000345, 2025-10-06T14:30:00.000345Z",
        "2025-10-06T08:04:49.297889, 2025-10-06T08:04:49.297889Z",
        "2025-10-06T08:04:49.29788, 2025-10-06T08:04:49.29788Z",
        "2025-10-06T08:04:49.2978, 2025-10-06T08:04:49.2978Z",
        "2025-10-06T08:04:49.297, 2025-10-06T08:04:49.297Z",
        "2025-10-06T08:04:49.29, 2025-10-06T08:04:49.29Z",
        "2025-10-06T08:04:49.2, 2025-10-06T08:04:49.2Z",

    })
    public void handlesAltinnValue(String date, String expectedDate) {

        OffsetDateTime offsetDateTime = objectMapper.readValue("\"" + date + "\"", OffsetDateTime.class);

        var expected = OffsetDateTime.parse(expectedDate);

        assertEquals(expected, offsetDateTime, "Should be able to deserialize datetime received from altinn that is not deserializable by default");
    }
}
