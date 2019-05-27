package no.difi.meldingsutveksling.receipt.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

@Configuration
public class SystemClockConfig {

    @Bean
    @Primary
    public Clock clock() {
        return Clock.fixed(Instant.parse("2019-03-25T11:38:23Z"), ZoneId.of("Europe/Oslo"));
    }
}
