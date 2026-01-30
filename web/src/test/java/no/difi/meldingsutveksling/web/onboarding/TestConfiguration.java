package no.difi.meldingsutveksling.web.onboarding;

import no.difi.meldingsutveksling.web.FrontendFunctionality;
import no.difi.meldingsutveksling.web.FrontendFunctionalityFaker;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TestConfiguration {

    @Bean
    @ConditionalOnProperty(name = "use.frontend.faker", matchIfMissing = true)
    public FrontendFunctionality frontendFunctionality() {
        return new FrontendFunctionalityFaker(); // use the faker when testing
    }

}
