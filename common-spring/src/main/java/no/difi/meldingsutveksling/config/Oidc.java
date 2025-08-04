package no.difi.meldingsutveksling.config;

import lombok.Data;
import no.difi.move.common.config.KeystoreProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.net.URL;

// FIXME det finnes en temmelig lik OIDC klasse internt i IntegrasjonspunktProperties, reuse?

@Data
public class Oidc {
    private URL url;
    private String audience;
    private String clientId;
    @NestedConfigurationProperty
    private KeystoreProperties keystore;
}
