package no.difi.meldingsutveksling.ptv;

public class CorrespondenceAgencyConfiguration {

    private String externalServiceEditionCode;
    private String externalServiceCode;
    private String password;
    private String systemUserCode;
    private boolean notifyEmail;
    private boolean notifySms;
    private String notificationText;
    private String sender;
    private String nextbestFiledir;
    private String endpointUrl;

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

    public boolean isNotifyEmail() {
        return notifyEmail;
    }

    public boolean isNotifySms() {
        return notifySms;
    }

    public String getNotificationText() {
        return notificationText;
    }

    public String getSender() {
        return sender;
    }

    public String getNextbestFiledir() {
        return nextbestFiledir;
    }

    public String getEndpointUrl() {
        return endpointUrl;
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

        public Builder withNotifyEmail(boolean notifyEmail) {
            correspondenceAgencyConfiguration.notifyEmail = notifyEmail;
            return this;
        }

        public Builder withNotifySms(boolean notifySms) {
            correspondenceAgencyConfiguration.notifySms = notifySms;
            return this;
        }

        public Builder withNotificationText(String notificationText) {
            correspondenceAgencyConfiguration.notificationText = notificationText;
            return this;
        }

        public Builder withSender(String sender) {
            correspondenceAgencyConfiguration.sender = sender;
            return this;
        }

        public Builder withNextbestFiledir(String filedir) {
            correspondenceAgencyConfiguration.nextbestFiledir = filedir;
            return this;
        }

        public Builder withEndpointUrl(String endpointUrl) {
            correspondenceAgencyConfiguration.endpointUrl = endpointUrl;
            return this;
        }

        public CorrespondenceAgencyConfiguration build() {
            return correspondenceAgencyConfiguration;
        }
    }
}
