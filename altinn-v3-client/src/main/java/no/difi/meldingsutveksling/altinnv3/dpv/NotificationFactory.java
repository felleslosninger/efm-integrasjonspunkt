package no.difi.meldingsutveksling.altinnv3.dpv;


import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.nextmove.DpvVarselTransportType;
import no.difi.meldingsutveksling.nextmove.DpvVarselType;
import no.difi.meldingsutveksling.nextmove.NextMoveOutMessage;
import no.digdir.altinn3.correspondence.model.EmailContentType;
import no.digdir.altinn3.correspondence.model.InitializeCorrespondenceNotificationExt;
import no.digdir.altinn3.correspondence.model.NotificationChannelExt;
import no.digdir.altinn3.correspondence.model.NotificationTemplateExt;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.EnumMap;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class NotificationFactory {

    private final IntegrasjonspunktProperties props;
    private final Clock clock;
    private final DpvHelper dpvHelper;
    private final ServiceRegistryHelper serviceRegistryHelper;

    public InitializeCorrespondenceNotificationExt getNotification(NextMoveOutMessage message) {
        InitializeCorrespondenceNotificationExt notification = new InitializeCorrespondenceNotificationExt();

        notification.setNotificationChannel(getChannel(message));
        notification.setRequestedSendTime(OffsetDateTime.now(clock).plusMinutes(5));
        notification.setNotificationTemplate(NotificationTemplateExt.CUSTOM_MESSAGE);
        notification.setEmailBody(getNotificationText(message));
        notification.setSmsBody(getNotificationText(message));
        notification.setEmailContentType(EmailContentType.PLAIN);
        notification.setEmailSubject(getEmailSubject());

        if (shouldSendReminder(message)) {
            notification.sendReminder(true);
            notification.setReminderEmailContentType(EmailContentType.PLAIN);
            notification.setReminderEmailSubject(getEmailSubject());
            notification.setReminderNotificationChannel(getChannel(message));
            notification.setReminderEmailBody(getNotificationText(message));
            notification.setReminderSmsBody(getNotificationText(message));
        } else {
            notification.sendReminder(false);
        }

        return notification;
    }

    private String getEmailSubject(){
        return props.getDpv().getEmailSubject();
    }

    private String getNotificationText(NextMoveOutMessage message) {
        if (dpvHelper.isConfidential(message)) {
            return dpvHelper.getDpvSettings(message).flatMap(s -> !isNullOrEmpty(s.getTaushetsbelagtVarselTekst()) ? Optional.of(s.getTaushetsbelagtVarselTekst()) : Optional.empty())
                .orElse(props.getDpv().getSensitiveNotificationText())
                .replace("$reporterName$", serviceRegistryHelper.getSenderName(message));
        }
        return dpvHelper.getDpvSettings(message).flatMap(s -> !isNullOrEmpty(s.getVarselTekst()) ? Optional.of(s.getVarselTekst()) : Optional.empty())
            .orElse(props.getDpv().getNotificationText())
            .replace("$reporterName$", serviceRegistryHelper.getSenderName(message));
    }

    private boolean shouldSendReminder(NextMoveOutMessage message) {
        DpvVarselType varselType = dpvHelper.getDpvSettings(message).flatMap(s -> s.getVarselType() != null ? Optional.of(s.getVarselType()) : Optional.empty())
            .orElse(DpvVarselType.VARSEL_DPV_MED_REVARSEL);

        return varselType == DpvVarselType.VARSEL_DPV_MED_REVARSEL;
    }

    private NotificationChannelExt getChannel(NextMoveOutMessage message) {
        EnumMap<DpvVarselTransportType, NotificationChannelExt> transportMap = new EnumMap<>(DpvVarselTransportType.class);
        transportMap.put(DpvVarselTransportType.EPOST, NotificationChannelExt.EMAIL);
        transportMap.put(DpvVarselTransportType.SMS, NotificationChannelExt.SMS);
        transportMap.put(DpvVarselTransportType.EPOSTOGSMS, NotificationChannelExt.EMAIL_AND_SMS);

        return dpvHelper.getDpvSettings(message)
            .flatMap(s -> s.getVarselTransportType() != null ? Optional.of(s.getVarselTransportType()) : Optional.empty())
            .map(transportMap::get)
            .orElseGet(() -> {
                if (props.getDpv().isNotifyEmail() && props.getDpv().isNotifySms()) {
                    return NotificationChannelExt.EMAIL_AND_SMS;
                } else if (props.getDpv().isNotifySms()) {
                    return NotificationChannelExt.SMS;
                } else if (props.getDpv().isNotifyEmail()) {
                    return NotificationChannelExt.EMAIL;
                } else {
                    return NotificationChannelExt.EMAIL_AND_SMS;
                }
            });
    }

    private boolean isNullOrEmpty(String s) {
        return s == null || s.isEmpty();
    }
}
