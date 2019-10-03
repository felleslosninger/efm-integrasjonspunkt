package no.difi.meldingsutveksling.noarkexchange.altinn;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.AltinnWsClient;
import no.difi.meldingsutveksling.AltinnWsClientFactory;
import no.difi.meldingsutveksling.FileReference;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.nextmove.NextMoveRuntimeException;

import javax.annotation.PostConstruct;
import java.util.List;

@RequiredArgsConstructor
public class AltinnConnectionCheck {

    private final IntegrasjonspunktProperties properties;
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

    private AltinnWsClient getAltinnWsClient() {
        return altinnWsClientFactory.getAltinnWsClient();
    }

}
