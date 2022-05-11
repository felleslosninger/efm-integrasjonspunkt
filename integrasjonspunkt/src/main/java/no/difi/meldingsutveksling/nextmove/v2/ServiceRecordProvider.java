package no.difi.meldingsutveksling.nextmove.v2;

import com.google.common.base.Strings;
import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.ApiType;
import no.difi.meldingsutveksling.MessageType;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.exceptions.MissingMessageTypeException;
import no.difi.meldingsutveksling.exceptions.ReceiverDoesNotAcceptProcessException;
import no.difi.meldingsutveksling.exceptions.UnknownMessageTypeException;
import no.difi.meldingsutveksling.nextmove.BusinessMessage;
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

    ServiceRecord getServiceRecord(StandardBusinessDocument sbd) {
        return sbd.getBusinessMessage(BusinessMessage.class)
                .map(p -> getServiceRecord(sbd, p))
                .orElseThrow(MissingMessageTypeException::new);
    }

    private ServiceRecord getServiceRecord(StandardBusinessDocument sbd, BusinessMessage<?> businessMessage) {
        try {
            SRParameter.SRParameterBuilder parameterBuilder = SRParameter.builder(sbd.getReceiverIdentifier().getPrimaryIdentifier())
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
        MessageType messageType = MessageType.valueOf(sbd.getType(), ApiType.NEXTMOVE)
            .orElseThrow(() -> new UnknownMessageTypeException(sbd.getType()));

        // Allow empty receiver for DPI print
        if (sbd.getReceiverIdentifier() == null) {
            if (messageType == MessageType.PRINT) {
                return DPI;
            }
            return UNKNOWN;
        }

        return getServiceRecord(sbd).getServiceIdentifier();
    }
}
