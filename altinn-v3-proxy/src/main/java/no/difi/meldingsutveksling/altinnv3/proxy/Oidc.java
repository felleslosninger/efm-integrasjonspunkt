package no.difi.meldingsutveksling.altinnv3.proxy;

import no.difi.move.common.config.KeystoreProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("oidc")
public record Oidc(String clientId, String scopes, String url, String audience, KeystoreProperties keystore) {
}
