package no.difi.meldingsutveksling.web;

import java.util.List;

public class FrontendFunctionalityFaker implements FrontendFunctionality {

    @Override
    public boolean isFake() {
        return true;
    }

    @Override
    public List<String> getChannelsEnabled() {
        return List.of("DPO", "DPV", "DPI", "DPF", "DPFIO", "DPE");
    }

    @Override
    public List<Property> configuration() {
        return List.of(
            new Property("difi.move.org.number", "311780735", "Ditt organisasjonsnummer")
        );
    }

    @Override
    public List<Property> configurationDPO() {
        return List.of(
            new Property("difi.move.org.keystore.path", "file:/Users/thorej/src/2023-cert-test-virks/eformidling-test-auth.jks", "Keystore med virksomhetssertifikat"),
            new Property("difi.move.org.keystore.alias", "digdir-test-eformidling", "Sertifikatets alias i keystore"),
            new Property("difi.move.org.keystore.password", "**********", "Passord på keystore"),
            new Property("difi.move.dpo.oidc.authenticationType", "JWK", "Maskinporten autentiserings type"),
            new Property("difi.move.dpo.oidc.jwk.path", "classpath:311780735-sterk-ulydig-hund-da.jwk", "JWK for autentisering i maskinporten"),
            new Property("difi.move.dpo.oidc.clientId", "b590f149-d0ba-4fca-b367-bccd9e444a00", "Maskinporten clientId"),
            new Property("difi.move.dpo.systemUser.orgId", "0192:311780735", "Din systembrukers orgId"),
            new Property("difi.move.dpo.systemUser.name", "311780735_systembruker_hund", "Din systembrukers navn"),
            new Property("difi.move.dpo.reportees[0].orgId", "0192:313711218", "Første på vegne av systembrukers orgId"),
            new Property("difi.move.dpo.reportees[0].name", "313711218_systembruker_ape", "Første på vegne av systembrukers navn")
        );
    }

}
