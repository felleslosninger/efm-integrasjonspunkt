package no.difi.meldingsutveksling.nextmove.v2;

import no.difi.meldingsutveksling.clock.ClockConfig;
import no.difi.meldingsutveksling.config.JacksonConfig;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import tools.jackson.databind.json.JsonMapper;

@Profile("!cucumber")
@TestConfiguration
@Import({JacksonConfig.class, ClockConfig.class})
public class JacksonTestConfig {

    /**
     * Test-slices (@DataJpaTest o.l.) autokonfigurerer ikkje Jackson, så mapperen må byggast
     * her. Customizer-bønnene blir brukte, slik at testane får same mapper-oppsett som
     * produksjon (jf. {@link JacksonConfig}).
     */
    @Bean
    @ConditionalOnMissingBean
    public JsonMapper jsonMapper(ObjectProvider<JsonMapperBuilderCustomizer> customizers) {
        JsonMapper.Builder builder = JsonMapper.builder();
        customizers.orderedStream().forEach(customizer -> customizer.customize(builder));
        return builder.build();
    }
}
