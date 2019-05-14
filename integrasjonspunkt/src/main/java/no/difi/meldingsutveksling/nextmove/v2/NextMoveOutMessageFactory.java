package no.difi.meldingsutveksling.nextmove.v2;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.ApiType;
import no.difi.meldingsutveksling.DocumentType;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.UUIDGenerator;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.Organisasjonsnummer;
import no.difi.meldingsutveksling.domain.sbdh.DocumentIdentification;
import no.difi.meldingsutveksling.domain.sbdh.PartnerIdentification;
import no.difi.meldingsutveksling.domain.sbdh.Sender;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.exceptions.UnknownNextMoveDocumentTypeException;
import no.difi.meldingsutveksling.nextmove.DpiPrintMessage;
import no.difi.meldingsutveksling.nextmove.NextMoveOutMessage;
import no.difi.meldingsutveksling.nextmove.PostAddress;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.ZonedDateTime;

import static com.google.common.base.Strings.isNullOrEmpty;

@Component
@RequiredArgsConstructor
public class NextMoveOutMessageFactory {

    private final IntegrasjonspunktProperties properties;
    private final NextMoveServiceRecordProvider serviceRecordProvider;
    private final UUIDGenerator uuidGenerator;
    private final Clock clock;

    NextMoveOutMessage getNextMoveOutMessage(StandardBusinessDocument sbd) {
        ServiceRecord serviceRecord = serviceRecordProvider.getServiceRecord(sbd);

        setDefaults(sbd, serviceRecord);

        return new NextMoveOutMessage(
                sbd.getConversationId(),
                sbd.getReceiverIdentifier(),
                sbd.getSenderIdentifier(),
                serviceRecord.getServiceIdentifier(),
                sbd);
    }

    private void setDefaults(StandardBusinessDocument sbd, ServiceRecord serviceRecord) {
        sbd.getScopes()
                .stream()
                .filter(p -> isNullOrEmpty(p.getInstanceIdentifier()))
                .forEach(p -> p.setInstanceIdentifier(uuidGenerator.generate()));

        if (sbd.getSenderIdentifier() == null) {
            Organisasjonsnummer org = Organisasjonsnummer.from(properties.getOrg().getNumber());
            sbd.getStandardBusinessDocumentHeader().addSender(
                    new Sender()
                            .setIdentifier(new PartnerIdentification()
                                    .setValue(org.asIso6523())
                                    .setAuthority(org.authority()))
            );
        }

        DocumentIdentification documentIdentification = sbd.getStandardBusinessDocumentHeader().getDocumentIdentification();

        if (documentIdentification.getInstanceIdentifier() == null) {
            documentIdentification.setInstanceIdentifier(uuidGenerator.generate());
        }

        if (documentIdentification.getCreationDateAndTime() == null) {
            documentIdentification.setCreationDateAndTime(ZonedDateTime.now(clock));
        }

        if (serviceRecord.getServiceIdentifier() == ServiceIdentifier.DPI) {
            setDpiDefaults(sbd, serviceRecord);
        }
    }

    private void setDpiDefaults(StandardBusinessDocument sbd, ServiceRecord serviceRecord) {
        DocumentType documentType = DocumentType.valueOf(sbd.getMessageType(), ApiType.NEXTMOVE)
                .orElseThrow(() -> new UnknownNextMoveDocumentTypeException(sbd.getMessageType()));

        if (documentType == DocumentType.PRINT) {
            DpiPrintMessage dpiMessage = (DpiPrintMessage) sbd.getAny();
            if (dpiMessage.getReceiver() == null) {
                dpiMessage.setReceiver(new PostAddress());
            }

            setReceiverDefaults(dpiMessage.getReceiver(), serviceRecord.getPostAddress());
            setReceiverDefaults(dpiMessage.getMailReturn().getReceiver(), serviceRecord.getReturnAddress());
        }
    }

    private void setReceiverDefaults(PostAddress receiver, no.difi.meldingsutveksling.serviceregistry.externalmodel.PostAddress srReceiver) {
        if (isNullOrEmpty(receiver.getName())) {
            receiver.setName(srReceiver.getName());
        }
        if (isNullOrEmpty(receiver.getAddressLine1())) {
            receiver.setAddressLine1(srReceiver.getStreet());
        }
        if (isNullOrEmpty(receiver.getPostalCode())) {
            receiver.setPostalCode(srReceiver.getPostalCode());
        }
        if (isNullOrEmpty(receiver.getPostalArea())) {
            receiver.setPostalArea(srReceiver.getPostalArea());
        }
        if (isNullOrEmpty(receiver.getCountry())) {
            receiver.setCountryCode(srReceiver.getCountry());
        }
    }
}
