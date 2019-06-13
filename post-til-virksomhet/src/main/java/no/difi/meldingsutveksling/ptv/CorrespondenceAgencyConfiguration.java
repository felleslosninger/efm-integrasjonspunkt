package no.difi.meldingsutveksling.ptv;

import lombok.Data;

@Data
public class CorrespondenceAgencyConfiguration {

    private String externalServiceEditionCode;
    private String externalServiceCode;
    private String password;
    private String systemUserCode;
    private boolean notifyEmail;
    private boolean notifySms;
    private String notificationText;
    private String nextmoveFiledir;
    private String endpointUrl;
    private boolean allowForwarding;

}
