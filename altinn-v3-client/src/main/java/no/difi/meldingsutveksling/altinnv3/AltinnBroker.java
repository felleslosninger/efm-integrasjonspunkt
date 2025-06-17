package no.difi.meldingsutveksling.altinnv3;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.digdir.altinn3.broker.model.FileTransferInitalizeExt;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "difi.move.feature.enableDPO", havingValue = "true")
@RequiredArgsConstructor
public class AltinnBroker {

    private final BrokerApiClient brokerApiClient;

    public void send(StandardBusinessDocument sbd) {

    }

    public void send(StandardBusinessDocument sbd,  Resource encryptedAsic) {

        FileTransferInitalizeExt request = new FileTransferInitalizeExt();


        //UUID fileTransferId = brokerApiClient.initialize()
    }
}
