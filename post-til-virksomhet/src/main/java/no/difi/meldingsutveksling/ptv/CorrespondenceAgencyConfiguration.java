package no.difi.meldingsutveksling.ptv;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Builder
@Data
public class CorrespondenceAgencyConfiguration {

    private String externalServiceEditionCode;
    private String externalServiceCode;
    private String password;
    private String systemUserCode;
    private String languageCode;
    private LocalDateTime visibleDateTime;
    private LocalDateTime allowSystemDeleteTime;
    private boolean allowForwarding;
    private boolean notifyEmail;
    private boolean notifySms;
    private String notificationText;
    private String emailText;
    private String smsText;
    private String sender;
    private String nextmoveFiledir;
    private String endpointUrl;

}
