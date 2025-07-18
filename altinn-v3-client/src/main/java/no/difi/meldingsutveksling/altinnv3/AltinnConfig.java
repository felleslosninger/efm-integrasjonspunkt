package no.difi.meldingsutveksling.altinnv3;

import jakarta.inject.Inject;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.move.common.oauth.JwtTokenClient;
import no.difi.move.common.oauth.JwtTokenConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;

@Configuration
public class AltinnConfig {

    @Inject
    IntegrasjonspunktProperties integrasjonspunktProperties;

    @Bean
    public JwtTokenClient jwtTokenClient() {
        return new JwtTokenClient(new JwtTokenConfig(
            integrasjonspunktProperties.getOidc().getClientId(),
            integrasjonspunktProperties.getOidc().getUrl().toString(),
            integrasjonspunktProperties.getOidc().getAudience(),
            new ArrayList<>(),
            integrasjonspunktProperties.getOidc().getKeystore()
        ));
    }
}
