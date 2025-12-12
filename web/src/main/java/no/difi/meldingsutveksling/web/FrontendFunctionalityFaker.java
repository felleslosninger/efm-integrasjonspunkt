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
            new Property("difi.move.dpo.brokerserviceUrl", "https://platform.tt02.altinn.no/broker/api/v1", "Altinn Broker Service url"),
            new Property("difi.move.dpo.altinnTokenExchangeUrl", "https://platform.tt02.altinn.no/authentication/api/v1/exchange/maskinporten", "Altinn Token Exchange url"),
            new Property("difi.move.dpo.resource", "eformidling-dpo-meldingsutveksling", "Hvilken broker service ressurs benyttes"),
            new Property("difi.move.dpo.oidc.authenticationType", "JWK", "Maskinporten autentiserings type"),
            new Property("difi.move.dpo.oidc.jwk.path", "classpath:311780735-sterk-ulydig-hund-da.jwk", "JWK for autentisering i maskinporten"),
            new Property("difi.move.dpo.oidc.clientId", "b590f149-d0ba-4fca-b367-bccd9e444a00", "Maskinporten client-id"),
            new Property("difi.move.dpo.systemUser.orgId", "0192:311780735", "Din systembrukers org-id"),
            new Property("difi.move.dpo.systemUser.name", "311780735_systembruker_hund", "Din systembrukers navn"),
            new Property("difi.move.dpo.reportees[0].orgId", "0192:313711218", "På vegne av systembrukers org-id"),
            new Property("difi.move.dpo.reportees[0].name", "313711218_systembruker_ape", "På vegne av systembrukers navn")
        );
    }

    @Override
    public List<Property> configurationDPV() {
        return List.of();
    }

    @Override
    public List<Property> configurationDPI() {
        return List.of();
    }

}
