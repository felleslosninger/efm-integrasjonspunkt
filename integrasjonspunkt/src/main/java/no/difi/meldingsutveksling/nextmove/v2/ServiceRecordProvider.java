package no.difi.meldingsutveksling.nextmove.v2;

import com.google.common.base.Strings;
import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.MessageType;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.domain.sbdh.ScopeType;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.exceptions.MissingMessageTypeException;
import no.difi.meldingsutveksling.exceptions.ReceiverDoesNotAcceptProcessException;
import no.difi.meldingsutveksling.exceptions.UnknownMessageTypeException;
import no.difi.meldingsutveksling.nextmove.BusinessMessage;
import no.difi.meldingsutveksling.nextmove.NextMoveRuntimeException;
import no.difi.meldingsutveksling.serviceregistry.SRParameter;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookupException;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static no.difi.meldingsutveksling.ServiceIdentifier.*;


enum PARTICIPANT {
    SENDER,RECEIVER
}

@Component
@RequiredArgsConstructor
public class ServiceRecordProvider {

    private final ServiceRegistryLookup serviceRegistryLookup;

    ServiceRecord getServiceRecord(StandardBusinessDocument sbd,PARTICIPANT participant) {
        return sbd.getBusinessMessage(BusinessMessage.class)
                .map(p -> getServiceRecord(sbd,p,participant))
                .orElseThrow(MissingMessageTypeException::new);
    }


    private ServiceRecord getServiceRecord(StandardBusinessDocument sbd, BusinessMessage<?> businessMessage,PARTICIPANT participant) {
        try {
            String participanId = null;
            if (participant == PARTICIPANT.RECEIVER) { participanId = MessageType.valueOfType(sbd.getType()).get() == MessageType.DIALOGMELDING ?  sbd.getReceiverIdentifier().getIdentifier() : sbd.getReceiverIdentifier().getPrimaryIdentifier();
                var herID2 = sbd.getScope(ScopeType.RECEIVER_HERID2);
                if (herID2.isPresent()) {
                    participanId = herID2.get().getInstanceIdentifier();
                }
            }
            else if (participant == PARTICIPANT.SENDER) {
                var herID2 = sbd.getScope(ScopeType.SENDER_HERID2);
                if (herID2.isPresent()) {
                    participanId = herID2.get().getInstanceIdentifier();
                }
                else {
                    throw new UnsupportedOperationException("Fetching service record of sender is only supported for DPH , when HerID2 is supplied");
                }
            }
            else {
                throw new UnsupportedOperationException("Unknown particiant type: " + participant);
            }


            SRParameter.SRParameterBuilder parameterBuilder = SRParameter.builder( participanId )
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
        } catch (ServiceRegistryLookupException e) {
            throw new ReceiverDoesNotAcceptProcessException(sbd.getProcess(), e.getLocalizedMessage());
        }
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

        return getServiceRecord(sbd,PARTICIPANT.RECEIVER).getServiceIdentifier();
    }
}
