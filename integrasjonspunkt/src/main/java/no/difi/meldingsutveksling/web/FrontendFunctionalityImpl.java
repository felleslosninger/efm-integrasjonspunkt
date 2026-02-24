package no.difi.meldingsutveksling.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.altinnv3.systemregister.SystemregisterApiClient;
import no.difi.meldingsutveksling.altinnv3.systemregister.SystemregisterApiClient.AccessPackageEntry;
import no.difi.meldingsutveksling.altinnv3.systemregister.SystemregisterApiClient.SystemUserEntry;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@RequiredArgsConstructor
public class FrontendFunctionalityImpl implements FrontendFunctionality {

    private final IntegrasjonspunktProperties props;
    private final SystemregisterApiClient srac;

    @Override
    public String getOrganizationNumber() {
        return props.getOrg().getNumber();
    }

    @Override
    public Version getIntegrasjonspunktVersion() {
        return new Version("4.0.1", "DEV-SNAPSHOT", false);
    }

    @Override
    public List<String> getServicesEnabled() {
        var channels = new ArrayList<String>();
        if (props.getFeature().isEnableDPO()) channels.add("DPO");
        if (props.getFeature().isEnableDPV()) channels.add("DPV");
        if (props.getFeature().isEnableDPI()) channels.add("DPI");
        if (props.getFeature().isEnableDPF()) channels.add("DPF");
        if (props.getFeature().isEnableDPFIO()) channels.add("DPFIO");
        if (props.getFeature().isEnableDPE()) channels.add("DPE");
        // FIXME add the DPH channel later
        return channels;
    }

    @Override
    public List<Property> configuration() {
        var config = new ArrayList<Property>();

        config.addAll(List.of(
            new Property("difi.move.org.number", props.getOrg().getNumber(), "Ditt organisasjonsnummer"),
            new Property("difi.move.serviceregistryEndpoint", props.getServiceregistryEndpoint(), "Service Registry Endpoint")
        ));

        var arkivmelding = props.getArkivmelding();
        if (arkivmelding != null) config.addAll(List.of(
            new Property("difi.move.arkivmelding.default-process", arkivmelding.getDefaultProcess(), "Arkivmelding Process"),
            new Property("difi.move.arkivmelding.default-document-type", arkivmelding.getDefaultDocumentType(), "Arkivmelding Document Type"),
            new Property("difi.move.arkivmelding.receipt-process", arkivmelding.getReceiptProcess(), "Arkivkvittering Process"),
            new Property("difi.move.arkivmelding.receipt-document-type", arkivmelding.getReceiptDocumentType(), "Arkivkvittering Document Type"),
            new Property("difi.move.arkivmelding.generate-arkivmelding-receipts", "" + arkivmelding.isGenerateArkivmeldingReceipts(), "Generer arkivmelding kvitteringer automatisk")
        ));

        return config;
    }

    @Override
    public List<Property> dpoConfiguration() {
        var config = new ArrayList<Property>();

        var dpo = props.getDpo();
        if (dpo != null) config.addAll(List.of(
            new Property("difi.move.dpo.brokerserviceUrl", dpo.getBrokerserviceUrl(), "Altinn Broker Service url"),
            new Property("difi.move.dpo.altinnTokenExchangeUrl", dpo.getAltinnTokenExchangeUrl(), "Altinn Token Exchange url"),
            new Property("difi.move.dpo.resource", dpo.getResource(), "Hvilken broker service ressurs benyttes"),
            new Property("difi.move.dpo.systemRegisterUrl", dpo.getSystemRegisterUrl(), "Altinn System Register url"),
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
            new Property("difi.move.dpo.oidc.keystore.password", mask(keystore.getPassword()), "Sertifikat keystore passord"),
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
            new Property("difi.move.dpo.systemName", dpo.getSystemName(), "Ditt system i Altinn"),
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
    public String dpoClientId() {
        return props.getDpo().getOidc().getClientId();
    }

    @Override
    public List<String> dpoSystemAccessPackages(String systemName) {
        try {
            return srac.getSystem(systemName).accessPackages().stream()
                .map(AccessPackageEntry::urn)
                .toList();
        } catch (Throwable e) {
            log.info("Failed to find access packages for system {}.", systemName, e);
        }
        return null;
    }

    @Override
    public boolean dpoSystemUserExists(String systemName, String systemUserName) {
        try {
            return dpoSystemUsersForSystem(systemName).stream().anyMatch(it -> it.equals(systemUserName));
        } catch (Throwable e) {
            log.info("Failed to find system user {} with required access packages.", systemUserName, e);
        }
        return false;
    }

    @Override
    public List<String> dpoSystemUsersForSystem(String systemName) {
        try {
            return srac.getAllSystemUsers(systemName).stream()
                .map(SystemUserEntry::externalRef)
                .toList();
        } catch (Throwable e) {
            log.info("Failed to get find system users for system {}", systemName, e);
        }
        return List.of();
    }

    @Override
    public boolean dpoCreateSystem(String systemName, String accessPackage) {
        log.info("Creating system for DPO with name {}", systemName);
        try {
            srac.createSystem(systemName);
            srac.updateAccessPackage(systemName, accessPackage);
            log.info("Successfully created system for DPO with name {} and access package {}", systemName, accessPackage);
        } catch (Throwable e) {
            log.info("Failed to create system for DPO with name {} and access package {}", systemName, accessPackage, e);
        }
        return false;
    }

    @Override
    public String dpoCreateSystemUser(String systemUserName, String systemName, String orgNo, String accessPackage) {
        log.info("Creating system user for DPO with name '{}'", systemUserName);
        String confirmSystemUserUrl = null;
        try {
            var details = srac.createStandardSystemUser(systemUserName, systemName, orgNo, accessPackage);
            confirmSystemUserUrl = details.confirmUrl();
            log.info("Successfully initated system user request with name {} for organization {}", details.externalRef(), details.partyOrgNo());
            log.info("Confirm system user request at : {}", confirmSystemUserUrl);
        } catch (Throwable e) {
            log.info("Failed to initate system user request with name {} for for organization {}", systemUserName, orgNo, e);
        }
        return confirmSystemUserUrl;
    }

    @Override
    public String dpoAccessToken(List<String> scopes) {
        if (scopes == null) scopes = List.of("altinn:serviceowner/data/read");
        try {
            return srac.getAccessToken(scopes);
        } catch (Throwable e) {
            log.info("Failed to get access token for scopes {}", scopes, e);
        }
        return null;
    }

    @Override
    public List<Property> dpvConfiguration() {
        var config = new ArrayList<Property>();

        var dpv = props.getDpv();
        if (dpv != null) config.addAll(List.of(
            new Property("difi.move.dpv.correspondenceServiceUrl", dpv.getCorrespondenceServiceUrl(), "Altinn Correspondence Service / Proxy url"),
            new Property("difi.move.dpv.healthCheckUrl", dpv.getHealthCheckUrl(), "Altinn Health Check url"),
            new Property("difi.move.dpv.altinnTokenExchangeUrl", dpv.getHealthCheckUrl(), "Altinn Token Exchange url"),
            new Property("difi.move.dpv.sensitiveResource", dpv.getSensitiveResource(), "Sensitve resource name"),
            new Property("difi.move.dpv.notifyEmail", "" + dpv.isNotifyEmail(), "Aktiver varsling på EMAIL"),
            new Property("difi.move.dpv.notifySms", "" + dpv.isNotifySms(), "Aktiver varsling på SMS"),
            new Property("difi.move.dpv.notificationText", dpv.getNotificationText(), "Varslingstekste"),
            new Property("difi.move.dpv.sensitiveNotificationText", dpv.getSensitiveNotificationText(), "Varslingstekst for sensitiv informasjon"),
            new Property("difi.move.dpv.emailSubject", dpv.getEmailSubject(), "Emne ved sending av epost varsel"),
            new Property("difi.move.dpv.enableDueDate", "" + dpv.isEnableDueDate(), "Aktiver utløpsdato"),
            new Property("difi.move.dpv.daysToReply", "" + dpv.getDaysToReply(), "Maks dager for svar"),
            new Property("difi.move.dpv.uploadSizeLimit", "" + dpv.getUploadSizeLimit().toBytes(), "Maks upload størrelse i bytes"),
            new Property("difi.move.dpv.defaultTtlHours", "" + dpv.getDefaultTtlHours(), "Standard levetid i timer")
        ));

        var oidc = dpv.getOidc();
        if (oidc != null) config.addAll(List.of(
            new Property("difi.move.dpv.oidc.url", oidc.getUrl().toString(), "Maskinporten autentiserings url"),
            new Property("difi.move.dpv.oidc.clientId", oidc.getClientId(), "Maskinporten client-id"),
            new Property("difi.move.dpv.oidc.audience", oidc.getAudience(), "Maskinporten audience"),
            new Property("difi.move.dpv.oidc.authenticationType", oidc.getAuthenticationType().name(), "Maskinporten autentiserings type")
        ));

        var keystore = oidc.getKeystore();
        if (keystore != null) config.addAll(List.of(
            new Property("difi.move.dpv.oidc.keystore.alias", keystore.getAlias(), "Sertifikat keystore alias"),
            new Property("difi.move.dpv.oidc.keystore.password", keystore.getPassword(), "Sertifikat keystore passord"),
            new Property("difi.move.dpv.oidc.keystore.type", keystore.getType(), "Sertifikat keystore type"),
            new Property("difi.move.dpv.oidc.keystore.path", keystore.getPath().toString(), "Sertifikat keystore path"),
            new Property("difi.move.dpv.oidc.keystore.lockProvider", keystore.getLockProvider().toString(), "Sertifikat keystore lock provider")
        ));

        var jwk = oidc.getJwk();
        if (jwk != null) config.addAll(List.of(
            new Property("difi.move.dpv.oidc.jwk.path", jwk.getPath().toString(), "JWK for autentisering i maskinporten")
        ));

        return config;
    }

    @Override
    public String dpvAccessToken() {
        return "AccessToken funksjon ikke implementert for DPV";
    }

    @Override
    public List<Property> dpiConfiguration() {
        var config = new ArrayList<Property>();

        var dpi = props.getDpi();
        if (dpi != null) config.addAll(List.of(
            new Property("difi.move.dpi.mpcId", dpi.getMpcId(), "DPI kanal for sending og mottak"),
            new Property("difi.move.dpi.certificate.mode", dpi.getCertificate().getMode(), "Sertifikat modus"),
            new Property("difi.move.dpi.upload-size-limit", "" + dpi.getUploadSizeLimit().toBytes(), "Maks upload størrelse i bytes")
        ));

        var asice = dpi.getAsice();
        if (asice != null) config.addAll(List.of(
            new Property("difi.move.dpi.asice.type", asice.getType(), "Hvilken type ASICe skal benyttes")
        ));

        config.add(
            new Property("denne-listen-er-ikke-komplett", "FIXME", "Det er flere properties som ikke vises enda.")
        );

        return config;
    }

    @Override
    public String dpiAccessToken() {
        return "AccessToken funksjon ikke implementert for DPI";
    }

    // utility to just mask secrets like passwords
    private String mask(String text) {
        return text.replaceAll(".", "*");
    }

}
