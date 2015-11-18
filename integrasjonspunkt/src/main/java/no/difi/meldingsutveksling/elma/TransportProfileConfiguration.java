package no.difi.meldingsutveksling.elma;

import no.difi.vefa.peppol.common.model.TransportProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Arrays;
import java.util.List;

/**
 * @author Glenn Bech
 */

@Configuration
public class TransportProfileConfiguration {

    public static final TransportProfile TRANSPORT_PROFILE_ALTINN_PROD = new TransportProfile("bdxr-transport-altinn");
    public static final TransportProfile TRANSPORT_PROFILE_ALTINN_DEV = new TransportProfile("bdxr-transport-altinn-dev");

    @Autowired
    Environment environment;

    @Bean
    public TransportProfile getProfile() {
        List<String> activeProfiles = Arrays.asList(environment.getActiveProfiles());
        boolean isRunneingInDevEnv = activeProfiles.contains("dev");
        if (isRunneingInDevEnv) {
            return TRANSPORT_PROFILE_ALTINN_DEV;
        } else {
            return TRANSPORT_PROFILE_ALTINN_PROD;
        }
    }

}
