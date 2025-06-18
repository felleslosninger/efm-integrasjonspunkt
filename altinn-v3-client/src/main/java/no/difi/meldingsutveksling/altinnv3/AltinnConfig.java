package no.difi.meldingsutveksling.altinnv3;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.move.common.config.KeystoreProperties;
import no.difi.move.common.oauth.JwtTokenClient;
import no.difi.move.common.oauth.JwtTokenConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class AltinnConfig {

    private final IntegrasjonspunktProperties props;

    @Bean
    public JwtTokenClient jwtTokenClient() {

        // FIXME : props.getOidc().getKeystore()
        KeystoreProperties kp = new KeystoreProperties();
        kp.setAlias("digdir-test-eformidling");
        kp.setPassword("MeldingTeHumor2023!");
        kp.setPath(new FileSystemResource("/Users/thorej/src/2023-cert-test-virks/eformidling-test-auth.jks"));

        return new JwtTokenClient(new JwtTokenConfig(
            "a63cac91-3210-4c35-b961-5c7bf122345c",
            "https://test.maskinporten.no/token",
            "https://test.maskinporten.no/",
            List.of("altinn:broker.write", "altinn:broker.read", "altinn:serviceowner"),
            kp
        ));
    }

}
