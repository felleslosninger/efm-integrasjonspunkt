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
@Configuration
@Profile("dev")
public class ElmaDevConfig {

    no.difi.vefa.peppol.lookup.
    public static final String ELMA_ENDPOINT_KEY = "bdxr-transport-altinn-dev";
    private static final TransportProfile TRANSPORT_PROFILE_ALTINN_DEV = new TransportProfile(ELMA_ENDPOINT_KEY);

    @Bean
    public TransportProfile getTransportProfile() {
        return TRANSPORT_PROFILE_ALTINN_DEV;
    }

    @Bean
    public LookupClient getElmaLookupClient() {
        return LookupClientBuilder.forTest().endpointCertificateValidator(null).providerCertificateValidator(null).build();
    }
}
