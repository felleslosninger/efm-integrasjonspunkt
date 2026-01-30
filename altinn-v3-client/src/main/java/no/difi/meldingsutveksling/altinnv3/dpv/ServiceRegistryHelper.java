package no.difi.meldingsutveksling.altinnv3.dpv;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.Iso6523;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.sbdh.SBDUtil;
import no.difi.meldingsutveksling.nextmove.NextMoveOutMessage;
import no.difi.meldingsutveksling.serviceregistry.SRParameter;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookupException;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ServiceRegistryHelper {

    private final IntegrasjonspunktProperties props;
    private final ServiceRegistryLookup serviceRegistryLookup;

    public String getSenderName(NextMoveOutMessage msg) {
        String orgnr = SBDUtil.getPartIdentifier(msg.getSbd())
            .map(Iso6523::getOrganizationIdentifier)
            .orElse(props.getOrg().getNumber());
        return serviceRegistryLookup.getInfoRecord(orgnr).getOrganizationName();
    }

    public ServiceRecord getServiceRecord(NextMoveOutMessage message) {
        ServiceRecord serviceRecord;
        try {
            serviceRecord = serviceRegistryLookup.getServiceRecord(
                SRParameter.builder(message.getReceiverIdentifier())
                    .conversationId(message.getConversationId())
                    .process(message.getSbd().getProcess())
                    .build(),
                message.getSbd().getDocumentType());
        } catch (ServiceRegistryLookupException e) {
            throw new MeldingsUtvekslingRuntimeException("Could not get service record for receiver %s".formatted(message.getReceiverIdentifier()), e);
        }
        return serviceRecord;
    }

}
