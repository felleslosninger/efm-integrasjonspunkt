package no.difi.meldingsutveksling.altinnv3.token;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class AltinnConfiguration {

    @Bean
    public TokenService getTokenService() {
        return new MaskinportenTokenService();
    }

    @Bean
    public TokenExchangeService getAltinnTokenSwapper() {
        return new AltinnTokenExchangeService(RestClient.create());
    }

}
