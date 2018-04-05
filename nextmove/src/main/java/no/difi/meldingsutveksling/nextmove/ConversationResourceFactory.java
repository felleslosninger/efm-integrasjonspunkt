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
            case DPE_INNSYN:
            case DPE_RECEIPT:
            case DPE_DATA:
            default:
                break;
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
            EpostVarsel epostVarsel = EpostVarsel.of(isNullOrEmpty(emailTekst) ? "" : emailTekst);
            String smsTekst = props.getDpi().getSms().getVarslingstekst();
            SmsVarsel smsVarsel = SmsVarsel.of(isNullOrEmpty(smsTekst) ? "" : smsTekst);
            Varsler varsler = Varsler.of(epostVarsel, smsVarsel);
            DigitalPostInfo digitalPostInfo = DigitalPostInfo.builder()
                    .varsler(varsler)
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
