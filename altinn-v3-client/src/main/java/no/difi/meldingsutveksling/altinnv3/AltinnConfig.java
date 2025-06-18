package no.difi.meldingsutveksling.altinnv3;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.move.common.oauth.JwtTokenClient;
import no.difi.move.common.oauth.JwtTokenConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class AltinnConfig {
    private final IntegrasjonspunktProperties props;
//    private final NewTokenUtil newTokenUtil;

//    @Bean
//    public BrokerApiClient brokerApiClient() {
//        return new BrokerApiClient(newTokenUtil);
//    }

    @Bean
    public JwtTokenClient jwtTokenClient() {
        return new JwtTokenClient(new JwtTokenConfig(
            "a63cac91-3210-4c35-b961-5c7bf122345c",
            "https://test.maskinporten.no/token",
            "https://test.maskinporten.no",
            List.of("altinn:broker.write", "altinn:broker.read", "altinn:serviceowner"),
            props.getOidc().getKeystore()
        ));
    }
}


