package no.difi.meldingsutveksling.altinnv3.proxy.properties;

import jakarta.validation.constraints.NotNull;
import no.difi.move.common.config.KeystoreProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties("oidc")
@Validated
public record Oidc(
    @NotNull
    String clientId,
//    @NotNull
    String scopes,
    @NotNull
    String url,
    @NotNull
    String audience,
    @NotNull
    KeystoreProperties keystore
    ) {
}

