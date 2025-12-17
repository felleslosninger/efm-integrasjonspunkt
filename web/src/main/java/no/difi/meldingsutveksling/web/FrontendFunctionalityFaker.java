package no.difi.meldingsutveksling.web;

import java.util.List;

public class FrontendFunctionalityFaker implements FrontendFunctionality {

    @Override
    public boolean isFake() {
        return true;
    }

    @Override
    public String getOrganizationNumber() {
        return "311780735";
    }

    @Override
    public Version getIntegrasjonspunktVersion() {
        return new Version("4.0.0-beta", "DEV-SNAPSHOT", true);
    }

    @Override
    public List<String> getChannelsEnabled() {
        return List.of("DPO", "DPV", "DPI", "DPF", "DPFIO", "DPE", "DPH");
    }

    @Override
    public List<Property> configuration() {
        return List.of(
            new Property("difi.move.org.number", "311780735", "Ditt organisasjonsnummer"),
            new Property("difi.move.serviceregistryEndpoint", "https://test.eformidling.no/adressetjeneste", "Service Registry Endpoint"),
            new Property("difi.move.arkivmelding.default-process", "urn:no:difi:profile:arkivmelding:administrasjon:ver1.0", "Arkivmelding Process"),
            new Property("difi.move.arkivmelding.default-document-type", "urn:no:difi:arkivmelding:xsd::arkivmelding", "Arkivmelding Document Type"),
            new Property("difi.move.arkivmelding.receipt-process", "urn:no:difi:profile:arkivmelding:response:ver1.0", "Arkivkvittering Process"),
            new Property("difi.move.arkivmelding.receipt-document-type", "urn:no:difi:arkivmelding:xsd::arkivmelding_kvittering", "Arkivkvittering Document Type"),
            new Property("difi.move.arkivmelding.generate-arkivmelding-receipts", "true", "Generer arkivmelding kvitteringer automatisk")
        );
    }

    @Override
    public List<Property> configurationDPO() {
        return List.of(
            new Property("difi.move.dpo.brokerserviceUrl", "https://platform.tt02.altinn.no/broker/api/v1", "Altinn Broker Service url"),
            new Property("difi.move.dpo.altinnTokenExchangeUrl", "https://platform.tt02.altinn.no/authentication/api/v1/exchange/maskinporten", "Altinn Token Exchange url"),
            new Property("difi.move.dpo.resource", "eformidling-dpo-meldingsutveksling", "Hvilken broker service ressurs benyttes"),
            new Property("difi.move.dpo.systemRegisterUrl", "https://platform.tt02.altinn.no/authentication/api/v1/systemregister", "Altinn System Register url"),
            new Property("difi.move.dpo.oidc.authenticationType", "JWK", "Maskinporten autentiserings type"),
            new Property("difi.move.dpo.oidc.jwk.path", "classpath:311780735-sterk-ulydig-hund-da.jwk", "JWK for autentisering i maskinporten"),
            new Property("difi.move.dpo.oidc.clientId", "b590f149-d0ba-4fca-b367-bccd9e444a00", "Maskinporten client-id"),
            new Property("difi.move.dpo.systemName", "311780735_integrasjonspunkt", "Ditt system i Altinn"),
            new Property("difi.move.dpo.systemUser.orgId", "0192:311780735", "Din systembrukers org-id"),
            new Property("difi.move.dpo.systemUser.name", "311780735_systembruker_hund", "Din systembrukers navn"),
            new Property("difi.move.dpo.reportees[0].orgId", "0192:313711218", "På vegne av systembrukers org-id"),
            new Property("difi.move.dpo.reportees[0].name", "313711218_systembruker_ape", "På vegne av systembrukers navn")
        );
    }

    @Override
    public List<Property> configurationDPV() {
        return List.of(
            new Property("difi.move.dpv.correspondenceServiceUrl", "https://eformidling.dev/altinn-proxy/correspondence/api/v1", "Altinn Correspondence Service Proxy url"),
            new Property("difi.move.dpv.healthCheckUrl", "https://platform.altinn.no/health-probe/", "Altinn Health Check url"),
            new Property("difi.move.dpv.altinnTokenExchangeUrl", "http://localhost:9800/altinntoken", "Altinn Token Exchange url"),
            new Property("difi.move.dpv.sensitiveResource", "eformidling-dpv-taushetsbelagt", "Sensitve resource name"),
            new Property("difi.move.dpv.notifyEmail", "true", "Aktiver varsling på EMAIL"),
            new Property("difi.move.dpv.notifySms", "false", "Aktiver varsling på SMS"),
            new Property("difi.move.dpv.email-subject", "Melding mottatt i Altinn", "Emne ved sending av epost varsel")
        );
    }

    @Override
    public List<Property> configurationDPI() {
        return List.of(
            new Property("difi.move.dpi.mpcId", "KANAL", "DPI kanal for sending og mottak"),
            new Property("difi.move.dpi.certificate.mode", "SELF_SIGNED", "Sertifikat modus"),
            new Property("difi.move.dpi.asice.type", "CMS", "Hvilken type ASICe skal benyttes"),
            new Property("difi.move.dpi.upload-size-limit", "150MB", "Maks upload størrelse"),
            new Property("denne-listen-er-ikke-komplett", "FIXME", "Det er flere properties som ikke vises enda.")
        );
    }

}
