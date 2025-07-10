package no.difi.meldingsutveksling.altinnv3.DPV;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Instant;

import static no.difi.meldingsutveksling.DateTimeUtil.DEFAULT_ZONE_ID;

@Configuration
public class DpvTestConfig {
    @Bean
    public Clock fixedClock() {
        return Clock.fixed(Instant.parse("2025-06-01T08:38:23Z"), DEFAULT_ZONE_ID);
    }
}
