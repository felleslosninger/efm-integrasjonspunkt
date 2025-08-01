package no.difi.meldingsutveksling.altinnv3;

import no.difi.meldingsutveksling.config.AltinnFormidlingsTjenestenConfig;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.move.common.oauth.JwtTokenClient;
import no.difi.move.common.oauth.JwtTokenConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;

@Configuration
public class AltinnConfiguration {

    @Bean
    public AltinnTokenSwapper getAltinnTokenSwapper(){
        return new AltinnTokenSwapper(RestClient.create());
    }

    @Bean
    @Qualifier("DpoJwtTokenClient")
    public JwtTokenClient getDpoJwtTokenClient(IntegrasjonspunktProperties properties) {
        AltinnFormidlingsTjenestenConfig props = properties.getDpo();

        JwtTokenConfig config = new JwtTokenConfig(
            props.getOidc().getClientId(),
            props.getOidc().getUrl().toString(),
            props.getOidc().getAudience(),
            new ArrayList<>(),
            props.getOidc().getKeystore()
        );

        return new JwtTokenClient(config);
    }

    @Bean
    @Qualifier("DpvJwtTokenClient")
    public JwtTokenClient getDpvJwtTokenClient(IntegrasjonspunktProperties properties) {
        AltinnFormidlingsTjenestenConfig props = properties.getDpo();

        JwtTokenConfig config = new JwtTokenConfig(
            props.getOidc().getClientId(),
            props.getOidc().getUrl().toString(),
            props.getOidc().getAudience(),
            new ArrayList<>(),
            props.getOidc().getKeystore()
        );

        return new JwtTokenClient(config);
    }
}
