package no.difi.meldingsutveksling.nextmove.v2;

import com.google.common.base.Strings;
import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.MessageType;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.domain.BusinessMessage;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.exceptions.MissingMessageTypeException;
import no.difi.meldingsutveksling.exceptions.ReceiverDoesNotAcceptProcessException;
import no.difi.meldingsutveksling.exceptions.UnknownMessageTypeException;
import no.difi.meldingsutveksling.nextmove.HasSikkerhetsNivaa;
import no.difi.meldingsutveksling.serviceregistry.SRParameter;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookupException;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.springframework.stereotype.Component;

import static no.difi.meldingsutveksling.ServiceIdentifier.DPI;
import static no.difi.meldingsutveksling.ServiceIdentifier.UNKNOWN;

@Component
@RequiredArgsConstructor
public class ServiceRecordProvider {

    private final ServiceRegistryLookup serviceRegistryLookup;

    public ServiceRecord getServiceRecord(StandardBusinessDocument sbd, Participant participant) {
        return sbd.getBusinessMessage(BusinessMessage.class)
            .map(p -> getServiceRecord(sbd, p, participant))
            .orElseThrow(MissingMessageTypeException::new);
    }

    private ServiceRecord getServiceRecord(StandardBusinessDocument sbd, BusinessMessage businessMessage, Participant participant) {
        try {
            String participantId = (participant == Participant.RECEIVER ? sbd.getReceiverIdentifier() : sbd.getSenderIdentifier()).getPrimaryIdentifier();
            return getServiceRecord(sbd, businessMessage, participantId);
        } catch (ServiceRegistryLookupException e) {
            throw new ReceiverDoesNotAcceptProcessException(sbd.getProcess(), e.getLocalizedMessage());
        }
    }

    private ServiceRecord getServiceRecord(StandardBusinessDocument sbd, BusinessMessage businessMessage, String participantId) throws ServiceRegistryLookupException {
        SRParameter.SRParameterBuilder parameterBuilder = SRParameter.builder(participantId)
            .process(sbd.getProcess());

        if (!Strings.isNullOrEmpty(sbd.getConversationId())) {
            parameterBuilder.conversationId(sbd.getConversationId());
        }
        if (businessMessage instanceof HasSikkerhetsNivaa<?> bm && bm.getSikkerhetsnivaa() != null) {
            parameterBuilder.securityLevel(bm.getSikkerhetsnivaa());
        }

        return serviceRegistryLookup.getServiceRecord(
            parameterBuilder.build(),
            sbd.getDocumentType());
    }


    ServiceIdentifier getServiceIdentifier(StandardBusinessDocument sbd) {
        MessageType messageType = MessageType.valueOf(sbd.getType(), null)
            .orElseThrow(() -> new UnknownMessageTypeException(sbd.getType()));

        // Allow empty receiver for DPI print
        if (sbd.getReceiverIdentifier() == null) {
            if (messageType == MessageType.PRINT) {
                return DPI;
            }
            return UNKNOWN;
        }

        return getServiceRecord(sbd, Participant.RECEIVER).getServiceIdentifier();
    }
}
