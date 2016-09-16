package no.difi.meldingsutveksling.ptv;

import org.springframework.core.env.Environment;

public class CorrespondenceAgencyConfiguration {
    private String externalServiceEditionCode;
    private String externalServiceCode;
    private String password;
    private String systemUserCode;
    private String endpointUrl;

    private CorrespondenceAgencyConfiguration() {
    }

    public static CorrespondenceAgencyConfiguration configurationFrom(Environment environment) {
        final CorrespondenceAgencyConfiguration configuration = new Builder(environment)
                .withExternalServiceCode("altinn.ptv.external_service_code")
                .withExternalServiceEditionCode("altinn.ptv.external_service_edition_code")
                .withSystemUserCode("altinn.ptv.user_code")
                .withPassword("altinn.ptv.password")
                .withEndpointURL("altinn.ptv.endpoint_url")
                .build();
        return configuration;
    }

    public String getExternalServiceEditionCode() {
        return externalServiceEditionCode;
    }

    public String getExternalServiceCode() {
        return externalServiceCode;
    }

    public String getSystemUserCode() {
        return systemUserCode;
    }

    public String getPassword() {
        return password;
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }

    public static class Builder {
        private final Environment environment;
        CorrespondenceAgencyConfiguration correspondenceAgencyConfiguration;
        public Builder(Environment environment) {
            this.environment = environment;
            correspondenceAgencyConfiguration = new CorrespondenceAgencyConfiguration();
        }

        public Builder withExternalServiceCode(String externalServiceCode) {
            correspondenceAgencyConfiguration.externalServiceCode = environment.getProperty(externalServiceCode);
            return this;
        }

        public Builder withExternalServiceEditionCode(String externalServiceEditionCode) {
            correspondenceAgencyConfiguration.externalServiceEditionCode = environment.getProperty(externalServiceEditionCode);
            return this;
        }

        public Builder withSystemUserCode(String systemUserCode) {
            correspondenceAgencyConfiguration.systemUserCode = environment.getProperty(systemUserCode);
            return this;
        }

        public Builder withPassword(String password) {
            correspondenceAgencyConfiguration.password = environment.getProperty(password);
            return this;
        }

        public Builder withEndpointURL(String url) {
            correspondenceAgencyConfiguration.endpointUrl = environment.getProperty(url);
            return this;
        }

        public CorrespondenceAgencyConfiguration build() {
            return correspondenceAgencyConfiguration;
        }
    }
}
