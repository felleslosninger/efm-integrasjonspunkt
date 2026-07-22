package no.difi.meldingsutveksling.nextmove.v2;

import no.difi.meldingsutveksling.clock.ClockConfig;
import no.difi.meldingsutveksling.config.JacksonConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import tools.jackson.databind.json.JsonMapper;

@Profile("!cucumber")
@TestConfiguration
@Import({JacksonConfig.class, ClockConfig.class})
public class JacksonTestConfig {

    @Bean
    @ConditionalOnMissingBean
    public JsonMapper jsonMapper() {
        return JsonMapper.builder().build();
    }
}
