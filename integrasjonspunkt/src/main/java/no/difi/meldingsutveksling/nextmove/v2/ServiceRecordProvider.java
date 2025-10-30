package no.difi.meldingsutveksling.nextmove.v2;

import com.google.common.base.Strings;
import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.MessageType;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.domain.NhnIdentifier;
import no.difi.meldingsutveksling.domain.sbdh.ScopeType;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.exceptions.IdentifierNotFoundException;
import no.difi.meldingsutveksling.exceptions.MissingMessageTypeException;
import no.difi.meldingsutveksling.exceptions.ReceiverDoesNotAcceptProcessException;
import no.difi.meldingsutveksling.exceptions.UnknownMessageTypeException;
import no.difi.meldingsutveksling.exceptions.UnsupportedOperationStatusException;
import no.difi.meldingsutveksling.nextmove.BusinessMessage;
import no.difi.meldingsutveksling.serviceregistry.NotFoundInServiceRegistryException;
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

    public ServiceRecord getServiceRecord(StandardBusinessDocument sbd,Participant participant) {
        return sbd.getBusinessMessage(BusinessMessage.class)
                .map(p -> getServiceRecord(sbd,p,participant))
                .orElseThrow(MissingMessageTypeException::new);
    }

    private ServiceRecord getServiceRecord(StandardBusinessDocument sbd, BusinessMessage<?> businessMessage,Participant participant) {
        try {
            String participantId;
            MessageType messageType = MessageType.valueOfType(sbd.getType())
                .orElseThrow(() -> new  UnknownMessageTypeException(sbd.getType()));

            if (messageType == MessageType.DIALOGMELDING) {

                if (participant == Participant.RECEIVER) {
                    var herID2 = sbd.getScope(ScopeType.RECEIVER_HERID2);
                    var reciever = (NhnIdentifier) sbd.getReceiverIdentifier();
                    if (reciever.isFastlegeIdentifier()) {
                        participantId = reciever.getIdentifier();
                    }
                    else if (herID2.isPresent()) {
                        participantId = herID2.get().getInstanceIdentifier();
                    } else {
                        // If we decide not to use 404 RecieverDoesNotAcceptDocumentType
                        throw new IdentifierNotFoundException("Missing valid identifier and HerID2 definition for DIALOGMELDING");
                    }
                }
                else {
                    var herID2 = sbd.getScope(ScopeType.SENDER_HERID2);
                    if (herID2.isPresent()) {
                        participantId = herID2.get().getInstanceIdentifier();
                    }
                    else {
                        throw new UnsupportedOperationStatusException("Fetching service record of sender is only supported for DPH , when HerID2 is supplied");
                    }

                }
                return getServiceRecord(sbd,businessMessage,participantId);

            }
            else {
                try {
                    if (participant == Participant.SENDER)
                        throw new UnsupportedOperationStatusException("Fetching service record of sender is only supported for DPH , when HerID2 is supplied");
                    participantId = sbd.getReceiverIdentifier().getPrimaryIdentifier();
                    return getServiceRecord(sbd, businessMessage, participantId);
                } catch (ServiceRegistryLookupException e) {
                    throw new ReceiverDoesNotAcceptProcessException(sbd.getProcess(), e.getLocalizedMessage());
                }

            }
        } catch (NotFoundInServiceRegistryException e) {
            throw new IdentifierNotFoundException(e.getMessage());
        }
        catch (ServiceRegistryLookupException e) {
            throw new ReceiverDoesNotAcceptProcessException(sbd.getProcess(), e.getLocalizedMessage());
        }
    }

    private ServiceRecord getServiceRecord(StandardBusinessDocument sbd,BusinessMessage<?> businessMessage, String participantId) throws ServiceRegistryLookupException {
        SRParameter.SRParameterBuilder parameterBuilder = SRParameter.builder(participantId)
            .process(sbd.getProcess());

        if (!Strings.isNullOrEmpty(sbd.getConversationId())) {
            parameterBuilder.conversationId(sbd.getConversationId());
        }

        if (businessMessage.getSikkerhetsnivaa() != null) {
            parameterBuilder.securityLevel(businessMessage.getSikkerhetsnivaa());
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

        return getServiceRecord(sbd,Participant.RECEIVER).getServiceIdentifier();
    }
}
