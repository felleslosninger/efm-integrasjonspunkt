package no.difi.meldingsutveksling.ptv;

public class CorrespondenceAgencyConfiguration {

    private String externalServiceEditionCode;
    private String externalServiceCode;
    private String password;
    private String systemUserCode;

    private CorrespondenceAgencyConfiguration() {
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

    public static class Builder {

        CorrespondenceAgencyConfiguration correspondenceAgencyConfiguration;

        public Builder() {
            correspondenceAgencyConfiguration = new CorrespondenceAgencyConfiguration();
        }

        public Builder withExternalServiceCode(String externalServiceCode) {
            correspondenceAgencyConfiguration.externalServiceCode = externalServiceCode;
            return this;
        }

        public Builder withExternalServiceEditionCode(String externalServiceEditionCode) {
            correspondenceAgencyConfiguration.externalServiceEditionCode = externalServiceEditionCode;
            return this;
        }

        public Builder withSystemUserCode(String systemUserCode) {
            correspondenceAgencyConfiguration.systemUserCode = systemUserCode;
            return this;
        }

        public Builder withPassword(String password) {
            correspondenceAgencyConfiguration.password = password;
            return this;
        }

        public CorrespondenceAgencyConfiguration build() {
            return correspondenceAgencyConfiguration;
        }
    }
}
