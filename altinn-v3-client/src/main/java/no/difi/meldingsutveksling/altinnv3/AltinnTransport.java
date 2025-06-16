package no.difi.meldingsutveksling.altinnv3;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "difi.move.feature.enableDPO", havingValue = "true")
@RequiredArgsConstructor
public class AltinnTransport {

//    private final UUIDGenerator uuidGenerator;
//    private final AltinnWsClient client;

    public void send(StandardBusinessDocument sbd) {

    }

    public void send(StandardBusinessDocument sbd,  Resource encryptedAsic) {

    }

}
