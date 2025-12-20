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
    public List<String> getServicesEnabled() {
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
    public String dpoClientId() {
        return "b590f149-d0ba-4fca-b367-bccd9e444a00";
    }

    @Override
    public List<String> dpoSystemDetails() {
        return List.of("urn:altinn:accesspackage:informasjon-og-kommunikasjon");
    }

    @Override
    public List<String> dpoSystemUsersForSystem() {
        return List.of("311780735_systembruker_tiger", "311780735_systembruker_ape");
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
    public String dpoAccessToken(List<String> scopes) {
        return "eyJhbGciOiJSUzI1NiIsImtpZCI6IjcxOUFGOTRFNDQ1MzE0Q0RDMjk1Rjk1MjUzODU4MDU0RjhCQ0FDODYiLCJ4NXQiOiJjWnI1VGtSVEZNM0NsZmxTVTRXQVZQaThySVkiLCJ0eXAiOiJKV1QifQ.eyJzY29wZSI6ImFsdGlubjpicm9rZXIud3JpdGUgYWx0aW5uOmJyb2tlci5yZWFkIiwidG9rZW5fdHlwZSI6IkJlYXJlciIsImV4cCI6MTc2NTk4MDk3MiwiaWF0IjoxNzY1OTc5MTcyLCJjbGllbnRfaWQiOiJhNjNjYWM5MS0zMjEwLTRjMzUtYjk2MS01YzdiZjEyMjM0NWMiLCJqdGkiOiI4dTZlbGg0N3hFdmdCU0trcF9uQkdmdmtURFk4OFBJTUNidHRRamtVRC1VIiwiY29uc3VtZXIiOnsiYXV0aG9yaXR5IjoiaXNvNjUyMy1hY3RvcmlkLXVwaXMiLCJJRCI6IjAxOTI6OTkxODI1ODI3In0sInVybjphbHRpbm46b3JnTnVtYmVyIjoiOTkxODI1ODI3IiwidXJuOmFsdGlubjphdXRoZW50aWNhdGVtZXRob2QiOiJtYXNraW5wb3J0ZW4iLCJ1cm46YWx0aW5uOmF1dGhsZXZlbCI6MywiaXNzIjoiaHR0cHM6Ly9wbGF0Zm9ybS50dDAyLmFsdGlubi5uby9hdXRoZW50aWNhdGlvbi9hcGkvdjEvb3BlbmlkLyIsInNpZCI6ImI4YzkxNzEwLTkyMTktNGRiYy1iOTkzLTg3MmNmMWExMWMyZCIsIm5iZiI6MTc2NTk3OTE3Mn0.qpBYnHgSvwBr0Tf5RAynUwgzDf3hv6GObyEPs8ncLeLSD_Tt5WLxThzNvapmouJoOOS0_6Qf1NL1cFVk6QFagn-pVbh8jw-PrOmtsfPpnatQc6gL6xdb_ZKQ5e2HUjxXvWhy-TIBiXUffPsc_gXK1Wef3ykLi2UiyBwAq1PjxQTpyF_0HOoE-K5GTm81lGE2wFHgmRnf3KmVhZm5-m3LbGdyPd2_9qdjsz5TXbKnXLZ-O2BdTUigGUaLsWZgjyd8SADHYo_zMSYSIg2vQ6rNh0qJsWjAwSmp6DJ1yYDJw12a2UmEF6ONX3Uv1-2wj5akX3BYymSxItQp7J_BAiCyDg";
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
    public String dpvAccessToken() {
        return "eyJhbGciOiJSUzI1NiIsImtpZCI6IjcxOUFGOTRFNDQ1MzE0Q0RDMjk1Rjk1MjUzODU4MDU0RjhCQ0FDODYiLCJ4NXQiOiJjWnI1VGtSVEZNM0NsZmxTVTRXQVZQaThySVkiLCJ0eXAiOiJKV1QifQ.eyJzY29wZSI6ImFsdGlubjpjb3JyZXNwb25kZW5jZS5yZWFkIGFsdGlubjpjb3JyZXNwb25kZW5jZS53cml0ZSIsInRva2VuX3R5cGUiOiJCZWFyZXIiLCJleHAiOjE3NjU5ODQ3MzcsImlhdCI6MTc2NTk4MjkzNywiY2xpZW50X2lkIjoiYTYzY2FjOTEtMzIxMC00YzM1LWI5NjEtNWM3YmYxMjIzNDVjIiwianRpIjoiR2xSWUVpbDV6RUt1OE52Y1YwMFNJczJvQmM2dXVhQUFzWXI3TnJxUGd4ZyIsImNvbnN1bWVyIjp7ImF1dGhvcml0eSI6ImlzbzY1MjMtYWN0b3JpZC11cGlzIiwiSUQiOiIwMTkyOjk5MTgyNTgyNyJ9LCJ1cm46YWx0aW5uOm9yZ051bWJlciI6Ijk5MTgyNTgyNyIsInVybjphbHRpbm46YXV0aGVudGljYXRlbWV0aG9kIjoibWFza2lucG9ydGVuIiwidXJuOmFsdGlubjphdXRobGV2ZWwiOjMsImlzcyI6Imh0dHBzOi8vcGxhdGZvcm0udHQwMi5hbHRpbm4ubm8vYXV0aGVudGljYXRpb24vYXBpL3YxL29wZW5pZC8iLCJzaWQiOiJlNTc5YmE2Ny04MjVlLTQyZDUtYTU5My00YWNlMjRmNjJjNzYiLCJuYmYiOjE3NjU5ODI5Mzd9.hzxNisEgWDJOQyUDrosstkLQCMC3oUBZv62Q0JhKZ6joOdOcBZFqQbsvBAv4Wv3Wja8_5U1s3yptdDM9azJXd91HQQG_5je8ENJW5RN1u2pnBytBE7rjTBgZVdV9UQImlX58fDcjgNBsWAla4oDepdLTFwenerH7dfTpFU-z38Chj1MA8Ct0oB_Crgrw17Im82et8swTryP678jp7rW47fL-rKxay8Uj0yhWoTbNFkRmpZMgrAWGnAwj_kEp7dol72g-p4YT0eX3Lyw7Y3Syv0rFKZAPg6iMRDNTPK1P7Qyo_NVmfNO9A4gV19rWLqcTFBZMC7m_RTq-UURl6XxcWw";
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

    @Override
    public String dpiAccessToken() {
        return "eyJraWQiOiJjWmswME1rbTVIQzRnN3Z0NmNwUDVGSFpMS0pzdzhmQkFJdUZiUzRSVEQ0IiwiYWxnIjoiUlMyNTYifQ.eyJzY29wZSI6ImRpZ2l0YWxwb3N0aW5uYnlnZ2VyOnNlbmQiLCJpc3MiOiJodHRwczpcL1wvdmVyMS5tYXNraW5wb3J0ZW4ubm9cLyIsImNsaWVudF9hbXIiOiJ2aXJrc29taGV0c3NlcnRpZmlrYXQiLCJ0b2tlbl90eXBlIjoiQmVhcmVyIiwiZXhwIjoxNjQ0ODcxMjQ1LCJpYXQiOjE2NDQ4NzExMjUsImNsaWVudF9pZCI6Ik1PVkVfSVBfOTkxODI1ODI3IiwianRpIjoiS1FIeU96SVNpVzQ3XzVsd19HajM4OHNPZS1sa2VlUlhtNVc1TVcwbUVCOCIsImNvbnN1bWVyIjp7ImF1dGhvcml0eSI6ImlzbzY1MjMtYWN0b3JpZC11cGlzIiwiSUQiOiIwMTkyOjk5MTgyNTgyNyJ9fQ.dISmO9LQV0t_yWXx-BJA2lCRfDuTCVMzkE2i0lxn7-OkhAoEedaxDdil-EjGGYAGHrUduZNrsM_AjXbJl3FeQOggs7md4X0-ccJ2FAlCYtjlG-CkN2xL868FH9f02kzFD5RCVcuJpyigIg8JQdBkb1Bseu8h0rjtVoSG6Qe17ZXWV6JQwb5qnQRqmFCrSWCpthL1wt-_zu5LFC2M8K0gE04a3QDWNEM6OaGLi9cFBGlMrXTVBr660uAae6UMtUo7CDGvpWRd9tYIXVzkzPbxJo1pKTCcWu8a_ZUXuVvAZ4NvNIGy7b4NzCEzldv-g73R2GUAz3GcERsHEN93CClusg";
    }

}
