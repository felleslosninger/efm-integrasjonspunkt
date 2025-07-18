package no.difi.meldingsutveksling.altinnv3.dpv;


import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.nextmove.DpvSettings;
import no.difi.meldingsutveksling.nextmove.DpvVarselType;
import no.difi.meldingsutveksling.nextmove.NextMoveOutMessage;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.Service;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import no.digdir.altinn3.correspondence.model.EmailContentType;
import no.digdir.altinn3.correspondence.model.InitializeCorrespondenceNotificationExt;
import no.digdir.altinn3.correspondence.model.NotificationChannelExt;
import no.digdir.altinn3.correspondence.model.NotificationTemplateExt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {
    NotificationFactory.class,
    DpvTestConfig.class
})
public class NotificationFactoryTest {
    @Autowired
    private NotificationFactory notificationFactory;

    @MockitoBean
    private IntegrasjonspunktProperties properties;

    @Autowired
    private Clock clock;

    @MockitoBean
    private Helper helper;

    private static final String SENDER_ORGNAME = "ACME Corp.";
    private static final String NOTIFICATION_TEXT = "Melding til $reporteeName$ fra $reporterName$";
    private static final String NOTIFICATION_TEXT_SENSITIVE = "Taushetsbelagt melding til $reporteeName$ fra $reporterName$";
    private static final String RESOURCE = "Resourceid";
    private static final String SENSITIVE_RESOURCE = "Sensitive Resourceid";
    private static final NextMoveOutMessage nextMoveOutMessage = new NextMoveOutMessage();
    private static final IntegrasjonspunktProperties.PostVirksomheter dpv = new IntegrasjonspunktProperties.PostVirksomheter();


    @BeforeEach
    void beforeEach() {
        dpv.setNotificationText(NOTIFICATION_TEXT);
        dpv.setSensitiveNotificationText(NOTIFICATION_TEXT_SENSITIVE);
        dpv.setSensitiveResource(SENSITIVE_RESOURCE);

        when(properties.getDpv()).thenReturn(dpv);
        when(helper.getServiceRecord(Mockito.any())).thenReturn(
            new ServiceRecord()
                .setService(new Service()
                    .setResource(RESOURCE)));
        when(helper.getSenderName(Mockito.any())).thenReturn(SENDER_ORGNAME);
    }

    @Test
    public void getNotification_mapsRequestedSendTime() {

        InitializeCorrespondenceNotificationExt notification = notificationFactory.getNotification(nextMoveOutMessage);

        assertEquals(OffsetDateTime.now(clock).plusMinutes(5), notification.getRequestedSendTime(), "RequestedSendTime should be 5 minutes after creation time");
    }

    @Test
    public void getNotification_mapsNotificationTemplate() {
        InitializeCorrespondenceNotificationExt notification = notificationFactory.getNotification(nextMoveOutMessage);
        assertEquals(NotificationTemplateExt.CUSTOM_MESSAGE, notification.getNotificationTemplate(), "NotificationTemplate should be Custom Message");
    }

    @Test
    public void getNotification_mapsEmailContentType() {
        InitializeCorrespondenceNotificationExt notification = notificationFactory.getNotification(nextMoveOutMessage);
        assertEquals(EmailContentType.PLAIN, notification.getEmailContentType(), "EmailContentType should be Plain");
    }

    @ParameterizedTest(name = "When message is {0} then the notification text should be {1}")
    @CsvSource({
        "not sensitive, Melding til $reporteeName$ fra ACME Corp.",
        "sensitive, Taushetsbelagt melding til $reporteeName$ fra ACME Corp."
    })
    public void getNotification_mapsNotificationText(String sensitive, String text){
        boolean isSensitive = sensitive.equals("sensitive");

        when(helper.isConfidential(Mockito.any())).thenReturn(isSensitive);

        InitializeCorrespondenceNotificationExt notification = notificationFactory.getNotification(nextMoveOutMessage);

        assertEquals(text, notification.getEmailBody());
        assertEquals(text, notification.getSmsBody());
    }

    @ParameterizedTest(name = "When message is {0}, and text is set in DpvSettings, then the notification text should be {1}")
    @CsvSource({
        "not sensitive, Melding fra ACME Corp.",
        "sensitive, Taushetsbelagt melding fra ACME Corp."
    })
    public void getNotification_mapsNotificationTextFromDpvSettings(String sensitive, String text){
        boolean isSensitive = sensitive.equals("sensitive");

        when(helper.isConfidential(Mockito.any())).thenReturn(isSensitive);
        Mockito.when(helper.getDpvSettings(Mockito.any())).thenReturn(Optional.of(
            new DpvSettings()
                .setVarselTekst("Melding fra $reporterName$")
                .setTaushetsbelagtVarselTekst("Taushetsbelagt melding fra $reporterName$")));


        InitializeCorrespondenceNotificationExt notification = notificationFactory.getNotification(nextMoveOutMessage);

        assertEquals(text, notification.getEmailBody());
        assertEquals(text, notification.getSmsBody());
    }


    @ParameterizedTest(name= "VarselType {0} in DpvSetting should make SendReminder be {1}")
    @CsvSource({
        "VARSEL_DPV_UTEN_REVARSEL, false, 'SendReminder should be false when setting is VARSEL_DPV_UTEN_REVARSEL'",
        "VARSEL_DPV_MED_REVARSEL, true, 'SendReminder should be true when setting is VARSEL_DPV_MED_REVARSEL'",
        ", true, 'Default value should be true'"
    })
    public void getNotification_mapsSendReminderBasedUponDpvSettings(DpvVarselType dpvVarsel, boolean sendReminder, String message) {
        Mockito.when(helper.getDpvSettings(Mockito.any())).thenReturn(Optional.of(new DpvSettings().setVarselType(dpvVarsel)));

        InitializeCorrespondenceNotificationExt notification = notificationFactory.getNotification(nextMoveOutMessage);

        assertEquals(sendReminder, notification.getSendReminder(), message);
    }

    @ParameterizedTest(name = "When NotifySms setting in properties is {0} and NotifyEmail is {0} then notificationChannel should be {1}")
    @CsvSource({
        "false, false, EMAIL_AND_SMS",
        "true, true, EMAIL_AND_SMS",
        "true, false, SMS",
        "false, true, EMAIL",
    })
    public void getNotification_mapsNotificationChannel(boolean notifySms, boolean notifyEmail, NotificationChannelExt notificationChannel) {
        dpv.setNotifySms(notifySms);
        dpv.setNotifyEmail(notifyEmail);

        InitializeCorrespondenceNotificationExt notification = notificationFactory.getNotification(nextMoveOutMessage);

        assertEquals(notificationChannel, notification.getNotificationChannel());
    }
}
