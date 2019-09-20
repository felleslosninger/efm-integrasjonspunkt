package no.difi.meldingsutveksling.noarkexchange.altinn;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.AltinnWsClient;
import no.difi.meldingsutveksling.AltinnWsClientFactory;
import no.difi.meldingsutveksling.FileReference;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.nextmove.NextMoveRuntimeException;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookupException;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;

import javax.annotation.PostConstruct;
import java.util.List;

import static no.difi.meldingsutveksling.ServiceIdentifier.DPO;

@RequiredArgsConstructor
public class AltinnConnectionCheck {

    private final IntegrasjonspunktProperties properties;
    private final ServiceRegistryLookup serviceRegistryLookup;
    private final AltinnWsClientFactory altinnWsClientFactory;

    @PostConstruct
    public void checkTheConnection() {
        try {
            AltinnWsClient client = getAltinnWsClient();
            List<FileReference> fileReferences = client.availableFiles(properties.getOrg().getNumber());
            if (fileReferences == null) {
                throw new NextMoveRuntimeException("Couldn't check for new messages from Altinn.");
            }
        } catch (Exception e) {
            throw new NextMoveRuntimeException("Couldn't check for new messages from Altinn.", e);
        }
    }

    private AltinnWsClient getAltinnWsClient() throws ServiceRegistryLookupException {
        return altinnWsClientFactory.getAltinnWsClient(getServiceRecord());
    }

    private ServiceRecord getServiceRecord() throws ServiceRegistryLookupException {
        return serviceRegistryLookup.getServiceRecord(properties.getOrg().getNumber(), DPO);
    }
}
