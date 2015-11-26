package no.difi.meldingsutveksling.elma;

import no.difi.vefa.peppol.common.model.TransportProfile;
import no.difi.vefa.peppol.lookup.LookupClient;
import no.difi.vefa.peppol.lookup.LookupClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * @author Glenn Bech
 */

@Profile("production")
@Configuration
public class ElmaProdConfig {
    private static final TransportProfile TRANSPORT_PROFILE_ALTINN = new TransportProfile("bdxr-transport-altinn");

    @Bean
    public TransportProfile getTransportProfile() {
        return TRANSPORT_PROFILE_ALTINN;
    }

    @Bean
    public LookupClient getElmaLookupClient() {
        return LookupClientBuilder.forProduction().build();
    }
}
