package no.difi.meldingsutveksling.nextmove.v2;

import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.nextmove.nhn.NhnAdapterClient;
import no.difi.meldingsutveksling.nhn.adapter.model.InMessage;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@Slf4j
public class NhnNextMoveMessageInService {
    private NhnAdapterClient nhnClient;


    public StandardBusinessDocument getMessageByHerId(Integer herId2) {
        List<InMessage> incomingMessages = nhnClient.incomingMessages(herId2,"23234");
        if (!incomingMessages.isEmpty()) {
            var firstIncoming =  incomingMessages.getFirst();
            nhnClient.incomingBusinessDocument(UUID.fromString(firstIncoming.getBusinessDocumentId()),"223423");
        }
    return null;
    }

}
