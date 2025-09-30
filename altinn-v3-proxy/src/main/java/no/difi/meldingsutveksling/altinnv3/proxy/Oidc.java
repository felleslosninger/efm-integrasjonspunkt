package no.difi.meldingsutveksling.altinnv3.proxy;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("oidc")
public record Oidc(String client, String scopes, Keystore keystore) {
    public record Keystore(String path) {}
}
