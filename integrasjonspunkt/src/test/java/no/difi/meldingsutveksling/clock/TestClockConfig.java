package no.difi.meldingsutveksling.clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Instant;

import static no.difi.meldingsutveksling.DateTimeUtil.DEFAULT_ZONE_ID;

@Configuration
public class TestClockConfig {

    @Bean
    public TestClock testClock() {
        return new TestClock(TestClock.fixed(Instant.parse("2019-03-25T11:38:23Z"), DEFAULT_ZONE_ID));
    }

}
