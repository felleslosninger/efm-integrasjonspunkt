package no.difi.meldingsutveksling.config;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import no.difi.move.common.config.KeystoreProperties;
import no.difi.move.common.config.JwkProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.net.URL;

@Data
public class Oidc {

    private URL url;
    private String audience;
    private String clientId;

    /**
     * Denne benyttes for å velge mellom CERTIFICATE (som bruker KeystoreProperties)
     * eller JWK (som bruker jwk nøkkelpar registrert på klienten i altinn).
     */
    @NotNull
    private AuthenticationType authenticationType = AuthenticationType.CERTIFICATE;

    /**
     * Properties for Certificate
     */
    @NestedConfigurationProperty
    private KeystoreProperties keystore;

    /**
     * Properties for JWK
     */
    @NestedConfigurationProperty
    private JwkProperties jwk;

}
