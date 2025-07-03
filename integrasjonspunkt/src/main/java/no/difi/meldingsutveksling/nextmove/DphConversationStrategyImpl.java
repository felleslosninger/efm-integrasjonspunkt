package no.difi.meldingsutveksling.nextmove;

import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.api.ConversationStrategy;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.nextmove.nhn.DPHMessageOut;
import no.difi.meldingsutveksling.nextmove.nhn.NhnAdapterClient;
import no.difi.meldingsutveksling.nextmove.nhn.Reciever;
import no.difi.meldingsutveksling.nextmove.nhn.Sender;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class DphConversationStrategyImpl implements ConversationStrategy {



    private NhnAdapterClient adapterClient;

    public DphConversationStrategyImpl(NhnAdapterClient adapterClient) {
        this.adapterClient = adapterClient;
    }

    @Override
    @Timed
    public void send(NextMoveOutMessage message) throws NextMoveException {
        DPHMessageOut messageOut = new DPHMessageOut("testMessageId","testConversationId",
            new Sender("testHerid1","testHerId2"), new Reciever("testHerId1","testHerId2"),"fagmelding");
        adapterClient.messageOut(messageOut);
        log.info("DphConversationStrategyImpl.send");

    }
}
