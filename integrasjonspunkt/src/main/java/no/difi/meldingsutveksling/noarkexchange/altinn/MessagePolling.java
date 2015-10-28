package no.difi.meldingsutveksling.noarkexchange.altinn;


import no.difi.meldingsutveksling.AltinnWsClient;
import no.difi.meldingsutveksling.AltinnWsConfiguration;
import no.difi.meldingsutveksling.DownloadRequest;
import no.difi.meldingsutveksling.FileReference;
import no.difi.meldingsutveksling.config.IntegrasjonspunktConfig;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.noarkexchange.IntegrajonspunktReceiveImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MessagePolling {
    Logger logger = LoggerFactory.getLogger(MessagePolling.class);

    @Autowired
    IntegrasjonspunktConfig config;

    @Autowired
    IntegrajonspunktReceiveImpl integrajonspunktReceive;

    @Scheduled(fixedRate = 15000)
    public void checkForNewMessages() {
        logger.debug("Checking for new messages");

        AltinnWsConfiguration configuration = AltinnWsConfiguration.fromConfiguration(config.getConfiguration());
        AltinnWsClient client = new AltinnWsClient(configuration);

        List<FileReference> fileReferences = client.availableFiles(config.getOrganisationNumber());

        for(FileReference reference : fileReferences) {
            StandardBusinessDocument sbd = client.download(new DownloadRequest(reference.getValue(), config.getOrganisationNumber()));
            //integrajonspunktReceive.receive(sbd);
        }

    }
}

