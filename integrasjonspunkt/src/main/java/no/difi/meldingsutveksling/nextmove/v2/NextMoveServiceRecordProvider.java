package no.difi.meldingsutveksling.nextmove.v2;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.exceptions.ReceiverDoNotAcceptProcessException;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookupException;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NextMoveServiceRecordProvider {

    private final ServiceRegistryLookup serviceRegistryLookup;

    ServiceRecord getServiceRecord(StandardBusinessDocument sbd) {
        try {
            return serviceRegistryLookup.getServiceRecord(sbd.getReceiverIdentifier(), sbd.getProcess(), sbd.getStandard());
        } catch (ServiceRegistryLookupException e) {
            throw new ReceiverDoNotAcceptProcessException(sbd.getProcess(), e.getLocalizedMessage());
        }
    }
}
