package no.difi.meldingsutveksling.nextmove.v2;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.exceptions.ReceiverDoNotAcceptProcessException;
import no.difi.meldingsutveksling.nextmove.BusinessMessage;
import no.difi.meldingsutveksling.serviceregistry.SRParameter;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookupException;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NextMoveServiceRecordProvider {

    private final ServiceRegistryLookup serviceRegistryLookup;

    ServiceRecord getServiceRecord(StandardBusinessDocument sbd) {
        BusinessMessage businessMessage = sbd.getBusinessMessage();
        try {
            SRParameter.SRParameterBuilder parameterBuilder = SRParameter.builder(sbd.getReceiverIdentifier())
                    .conversationId(sbd.getConversationId());
            if (businessMessage.getSikkerhetsnivaa() != null) {
                parameterBuilder.securityLevel(businessMessage.getSikkerhetsnivaa());
            }
            return serviceRegistryLookup.getServiceRecord(
                    parameterBuilder.build(),
                    sbd.getProcess(),
                    sbd.getStandard());
        } catch (ServiceRegistryLookupException e) {
            throw new ReceiverDoNotAcceptProcessException(sbd.getProcess(), e.getLocalizedMessage());
        }
    }
}
