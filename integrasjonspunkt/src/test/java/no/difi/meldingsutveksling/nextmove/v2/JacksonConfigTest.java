package no.difi.meldingsutveksling.nextmove.v2;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.difi.meldingsutveksling.clock.TestClock;
import no.difi.meldingsutveksling.clock.TestClockConfig;
import no.difi.meldingsutveksling.config.JacksonConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@JsonTest
@TestPropertySource("classpath:/config/application-test.properties")
@ActiveProfiles("test")
@Import({JacksonConfig.class, TestClockConfig.class})
public class JacksonConfigTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestClock testClock;

    @AfterEach
    public void after() {
        testClock.reset();
    }

    @Test
    public void testDeserializeLocalDateToOffsetDateTimeWhenWinterTime() throws Exception {
        assertThat(objectMapper.readValue("\"2019-03-25T11:38:23\"", OffsetDateTime.class))
                .isEqualTo(OffsetDateTime.parse("2019-03-25T11:38:23+01:00"));
    }

    @Test
    public void testDeserializeLocalDateToOffsetDateTimeWhenSummerTime() throws Exception {
        testClock.setActive("2019-05-01T11:38:23Z");
        assertThat(objectMapper.readValue("\"2019-05-01T11:38:23\"", OffsetDateTime.class))
                .isEqualTo(OffsetDateTime.parse("2019-05-01T11:38:23+02:00"));
    }

    @Test
    public void testDeserializeOffsetDateWithTwoHourOffset() throws Exception {
        assertThat(objectMapper.readValue("\"2019-03-25T11:38:23+02:00\"", OffsetDateTime.class))
                .isEqualTo(OffsetDateTime.parse("2019-03-25T11:38:23+02:00"));
    }

    @Test
    public void testDeserializeOffsetDateWithUTC() throws Exception {
        assertThat(objectMapper.readValue("\"2019-03-25T11:38:23Z\"", OffsetDateTime.class))
                .isEqualTo(OffsetDateTime.parse("2019-03-25T11:38:23Z"));
    }

    @Test
    public void testSerializeOffsetDateTime() throws Exception {
        assertThat(objectMapper.writeValueAsString(OffsetDateTime.parse("2019-03-25T11:38:23+02:00")))
                .isEqualTo("\"2019-03-25T11:38:23+02:00\"");
    }
}
