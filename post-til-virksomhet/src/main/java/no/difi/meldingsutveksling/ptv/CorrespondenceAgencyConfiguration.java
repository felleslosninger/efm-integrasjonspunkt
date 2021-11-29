package no.difi.meldingsutveksling.ptv;

import lombok.Data;

@Data
public class CorrespondenceAgencyConfiguration {

    private String password;
    private String systemUserCode;
    private String sensitiveServiceCode;
    private boolean notifyEmail;
    private boolean notifySms;
    private String notificationText;
    private String sensitiveNotificationText;
    private String nextmoveFiledir;
    private String endpointUrl;
    private boolean allowForwarding;

}
