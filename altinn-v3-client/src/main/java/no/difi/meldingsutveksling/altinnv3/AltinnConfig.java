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
            "a63cac91-3210-4c35-b961-5c7bf122345c", // FIXME move this to own config and use integrasjonspunktProperties.getOidc().getClientId()
            integrasjonspunktProperties.getOidc().getUrl().toString(),
            integrasjonspunktProperties.getOidc().getAudience(),
            new ArrayList<>(),
            integrasjonspunktProperties.getOidc().getKeystore()
        ));
    }

}
