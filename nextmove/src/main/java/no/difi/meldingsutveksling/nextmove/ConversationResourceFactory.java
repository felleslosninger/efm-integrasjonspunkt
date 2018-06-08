package no.difi.meldingsutveksling.nextmove;

import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.nextmove.dpi.DigitalPostInfo;
import no.difi.meldingsutveksling.nextmove.dpi.FysiskPostInfo;
import no.difi.meldingsutveksling.nextmove.dpi.PostRetur;
import no.difi.meldingsutveksling.nextmove.dpi.ReturMottaker;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.PostAddress;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import static com.google.common.base.Strings.isNullOrEmpty;
import static no.difi.meldingsutveksling.ServiceIdentifier.DPI;

@Component
@RequiredArgsConstructor
public class ConversationResourceFactory {

    private final ServiceRegistryLookup sr;
    private final IntegrasjonspunktProperties props;

    public void create(ConversationResource cr) {

        switch (cr.getServiceIdentifier()) {
            case DPI:
                setDpiDefaults((DpiConversationResource)cr);
                break;
            case DPF:
            case DPO:
            case DPV:
                setDpvDefaults((DpvConversationResource)cr);
            case DPE_INNSYN:
            case DPE_RECEIPT:
            case DPE_DATA:
            default:
                break;
        }
    }

    private void setDpvDefaults(DpvConversationResource cr) {
        if (isNullOrEmpty(cr.getServiceEdition())) {
            cr.setServiceEdition("10");
        }
        if (isNullOrEmpty(cr.getLanguageCode())) {
            cr.setLanguageCode("1044");
        }
        if (cr.getVisibleDateTime() == null) {
            cr.setVisibleDateTime(LocalDateTime.now());
        }
        if (cr.getAllowSystemDeleteDateTime() == null) {
            cr.setAllowSystemDeleteDateTime(LocalDateTime.now().plusMinutes(5));
        }
        if (cr.getNotifications() == null) {
            String notificationText = isNullOrEmpty(props.getDpv().getNotificationText()) ? "" : props.getDpv().getNotificationText();
            EmailNotification emailNotification = EmailNotification.of(notificationText);
            SmsNotification smsNotification = SmsNotification.of(notificationText);
            Notifications notifications = Notifications.of(emailNotification, smsNotification);
            cr.setNotifications(notifications);
        }
        if (cr.getFiles() == null) {
            cr.setFiles(Lists.newArrayList());
        }
    }

    private void setDpiDefaults(DpiConversationResource cr) {

        Optional<ServiceRecord> serviceRecord = sr.getServiceRecord(cr.getReceiver().getReceiverId(), DPI, cr.isMandatoryNotification());
        if (!serviceRecord.isPresent()) {
            throw new NextMoveRuntimeException(String.format("Could not find DPI servicerecord for receiver: %s", cr.getReceiver().getReceiverId()));
        }

        if (isNullOrEmpty(cr.getSpraak())) {
            cr.setSpraak(props.getDpi().getLanguage());
        }

        serviceRecord.map(ServiceRecord::getPostAddress)
                .map(PostAddress::getName)
                .ifPresent(n -> cr.getReceiver().setReceiverName(n));
        if (serviceRecord.get().isFysiskPost() && cr.getFysiskPostInfo() == null) {
            ReturMottaker returMottaker = ReturMottaker.builder()
                    .navn(serviceRecord.get().getReturnAddress().getName())
                    .adresselinje1(serviceRecord.get().getReturnAddress().getStreet())
                    .postnummer(serviceRecord.get().getReturnAddress().getPostalCode())
                    .poststed(serviceRecord.get().getReturnAddress().getPostalArea())
                    .land(serviceRecord.get().getReturnAddress().getCountry())
                    .build();
            PostRetur postRetur = new PostRetur(returMottaker, props.getDpi().getPrintSettings().getReturnType().toExternal());
            FysiskPostInfo fysiskPostInfo = FysiskPostInfo.builder()
                    .utskriftsfarge(props.getDpi().getPrintSettings().getInkType().toExternal())
                    .posttype(props.getDpi().getPrintSettings().getShippingType().toExternal())
                    .retur(postRetur)
                    .build();
            cr.setFysiskPostInfo(fysiskPostInfo);
        }

        if (!serviceRecord.get().isFysiskPost() && cr.getDigitalPostInfo() == null) {
            String emailTekst = props.getDpi().getEmail().getVarslingstekst();
            EmailNotification emailNotification = EmailNotification.of(isNullOrEmpty(emailTekst) ? "" : emailTekst);
            String smsTekst = props.getDpi().getSms().getVarslingstekst();
            SmsNotification smsNotification = SmsNotification.of(isNullOrEmpty(smsTekst) ? "" : smsTekst);
            Notifications notifications = Notifications.of(emailNotification, smsNotification);
            DigitalPostInfo digitalPostInfo = DigitalPostInfo.builder()
                    .notifications(notifications)
                    .virkningsdato(LocalDate.now())
                    .virkningstidspunkt(LocalTime.now())
                    .aapningskvittering(false)
                    .ikkeSensitivTittel("")
                    .build();
            cr.setDigitalPostInfo(digitalPostInfo);
            cr.setFiles(Lists.newArrayList());
        }

    }
}
