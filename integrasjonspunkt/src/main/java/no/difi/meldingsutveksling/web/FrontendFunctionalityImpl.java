package no.difi.meldingsutveksling.web;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@RequiredArgsConstructor
public class FrontendFunctionalityImpl implements FrontendFunctionality {

    private final IntegrasjonspunktProperties props;

    @Override
    public List<String> getChannelsEnabled() {
        var channels = new ArrayList<String>();
        if (props.getFeature().isEnableDPO()) channels.add("DPO");
        if (props.getFeature().isEnableDPV()) channels.add("DPV");
        if (props.getFeature().isEnableDPI()) channels.add("DPI");
        if (props.getFeature().isEnableDPF()) channels.add("DPF");
        if (props.getFeature().isEnableDPFIO()) channels.add("DPFIO");
        if (props.getFeature().isEnableDPE()) channels.add("DPE");
        return channels;
    }

    @Override
    public List<Property> configuration() {
        return List.of(
            new Property("difi.move.org.number", props.getOrg().getNumber(), "Ditt organisasjonsnummer")
        );
    }

    @Override
    public List<Property> configurationDPO() {
        var config = new ArrayList<Property>();

        var dpo = props.getDpo();
        if (dpo != null) config.addAll(List.of(
                new Property("difi.move.dpo.brokerserviceUrl", dpo.getBrokerserviceUrl(), "Altinn Broker Service url"),
                new Property("difi.move.dpo.altinnTokenExchangeUrl", dpo.getAltinnTokenExchangeUrl(), "Altinn Token Exchange url"),
                new Property("difi.move.dpo.resource", dpo.getResource(), "Hvilken broker service ressurs benyttes"),
                new Property("difi.move.dpo.connectTimeout", "" + dpo.getConnectTimeout(), "Connection timeout i millisekunder"),
                new Property("difi.move.dpo.requestTimeout", "" + dpo.getRequestTimeout(), "Request timeout i millisekunder"),
                new Property("difi.move.dpo.uploadSizeLimit", "" + dpo.getUploadSizeLimit(), "Maks upload størrelse i bytes"),
                new Property("difi.move.dpo.messageChannel", dpo.getMessageChannel(), "Kanal for overføring (hvis noen)"),
                new Property("difi.move.dpo.defaultTtlHours", "" + dpo.getDefaultTtlHours(), "Standard levetid i timer")
            ));

        var oidc = dpo.getOidc();
        if (oidc != null) config.addAll(List.of(
            new Property("difi.move.dpo.oidc.url", oidc.getUrl().toString(), "Maskinporten autentiserings url"),
            new Property("difi.move.dpo.oidc.clientId", oidc.getClientId(), "Maskinporten client-id"),
            new Property("difi.move.dpo.oidc.audience", oidc.getAudience(), "Maskinporten audience"),
            new Property("difi.move.dpo.oidc.authenticationType", oidc.getAuthenticationType().name(), "Maskinporten autentiserings type")
        ));

        var keystore = oidc.getKeystore();
        if (keystore != null) config.addAll(List.of(
            new Property("difi.move.dpo.oidc.keystore.alias", keystore.getAlias(), "Sertifikat keystore alias"),
            new Property("difi.move.dpo.oidc.keystore.password", keystore.getPassword(), "Sertifikat keystore passord"),
            new Property("difi.move.dpo.oidc.keystore.type", keystore.getType(), "Sertifikat keystore type"),
            new Property("difi.move.dpo.oidc.keystore.path", keystore.getPath().toString(), "Sertifikat keystore path"),
            new Property("difi.move.dpo.oidc.keystore.lockProvider", keystore.getLockProvider().toString(), "Sertifikat keystore lock provider")
        ));

        var jwk = oidc.getJwk();
        if (jwk != null) config.addAll(List.of(
            new Property("difi.move.dpo.oidc.jwk.path", jwk.getPath().toString(), "JWK for autentisering i maskinporten")
        ));

        var systemUser = dpo.getSystemUser();
        if (systemUser != null) config.addAll(List.of(
            new Property("difi.move.dpo.systemUser.orgId", systemUser.getOrgId(), "Din systembrukers org-id"),
            new Property("difi.move.dpo.systemUser.name", systemUser.getName(), "Din systembrukerss navn")
        ));

        AtomicInteger counter = new AtomicInteger(0);
        if (dpo.getReportees() != null) dpo.getReportees().forEach(
            su -> config.addAll(List.of(
                new Property("difi.move.dpo.reportees[%d].orgId".formatted(counter.get()), su.getOrgId(), "På vegne av systembrukers org-id"),
                new Property("difi.move.dpo.reportees[%d].name".formatted(counter.getAndIncrement()), su.getName(), "På vegne av systembrukers navn")
        )));

        return config;
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
