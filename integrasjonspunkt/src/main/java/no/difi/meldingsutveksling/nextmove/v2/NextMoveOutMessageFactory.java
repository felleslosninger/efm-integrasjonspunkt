package no.difi.meldingsutveksling.nextmove.v2;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.ApiType;
import no.difi.meldingsutveksling.DocumentType;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.UUIDGenerator;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.Organisasjonsnummer;
import no.difi.meldingsutveksling.domain.sbdh.*;
import no.difi.meldingsutveksling.exceptions.ReceiverDoNotAcceptProcessException;
import no.difi.meldingsutveksling.exceptions.UnknownNextMoveDocumentTypeException;
import no.difi.meldingsutveksling.nextmove.DpiPrintMessage;
import no.difi.meldingsutveksling.nextmove.NextMoveOutMessage;
import no.difi.meldingsutveksling.nextmove.PostAddress;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookupException;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
public class NextMoveOutMessageFactory {

    private final IntegrasjonspunktProperties properties;
    private final ServiceRegistryLookup serviceRegistryLookup;
    private final UUIDGenerator uuidGenerator;
    private final Clock clock;

    NextMoveOutMessage getNextMoveOutMessage(StandardBusinessDocument sbd) {
        ServiceRecord serviceRecord;
        try {
            serviceRecord = serviceRegistryLookup.getReceiverServiceRecord(sbd);
        } catch (ServiceRegistryLookupException e) {
            throw new ReceiverDoNotAcceptProcessException(sbd.getProcess(), e.getLocalizedMessage());
        }

        setDefaults(sbd, serviceRecord);

        return new NextMoveOutMessage(
                sbd.getConversationId(),
                sbd.getDocumentId(),
                sbd.getProcess(),
                sbd.getReceiverIdentifier(),
                sbd.getSenderIdentifier(),
                serviceRecord.getServiceIdentifier(),
                sbd);
    }

    private void setDefaults(StandardBusinessDocument sbd, ServiceRecord serviceRecord) {
        sbd.getScopes()
                .stream()
                .filter(p -> !StringUtils.hasText(p.getInstanceIdentifier()))
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
            documentIdentification.setCreationDateAndTime(OffsetDateTime.now(clock));
        }

        if (!sbd.getExpectedResponseDateTime().isPresent()) {
            OffsetDateTime ttl = OffsetDateTime.now(clock).plusHours(properties.getNextmove().getDefaultTtlHours());
            if (sbd.getScope(ScopeType.CONVERSATION_ID).getScopeInformation().isEmpty()) {
                CorrelationInformation ci = new CorrelationInformation().setExpectedResponseDateTime(ttl);
                sbd.getScope(ScopeType.CONVERSATION_ID).getScopeInformation().add(ci);
            } else {
                sbd.getScope(ScopeType.CONVERSATION_ID).getScopeInformation().stream()
                        .findFirst()
                        .ifPresent(ci -> ci.setExpectedResponseDateTime(ttl));
            }
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
            if (dpiMessage.getMottaker() == null) {
                dpiMessage.setMottaker(new PostAddress());
            }

            setReceiverDefaults(dpiMessage.getMottaker(), serviceRecord.getPostAddress());
            setReceiverDefaults(dpiMessage.getRetur().getMottaker(), serviceRecord.getReturnAddress());
        }
    }

    private void setReceiverDefaults(PostAddress receiver, no.difi.meldingsutveksling.serviceregistry.externalmodel.PostAddress srReceiver) {
        if (!StringUtils.hasText(receiver.getNavn())) {
            receiver.setNavn(srReceiver.getName());
        }
        if (!StringUtils.hasText(receiver.getAdresselinje1())) {
            receiver.setAdresselinje1(srReceiver.getStreet());
        }
        if (!StringUtils.hasText(receiver.getPostnummer())) {
            receiver.setPostnummer(srReceiver.getPostalCode());
        }
        if (!StringUtils.hasText(receiver.getPoststed())) {
            receiver.setPoststed(srReceiver.getPostalArea());
        }
        if (!StringUtils.hasText(receiver.getLand())) {
            receiver.setLand(srReceiver.getCountry());
        }
    }
}
