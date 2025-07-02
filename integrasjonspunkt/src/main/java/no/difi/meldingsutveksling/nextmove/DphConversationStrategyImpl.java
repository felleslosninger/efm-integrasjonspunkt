package no.difi.meldingsutveksling.nextmove;

import io.micrometer.core.annotation.Timed;
import no.difi.meldingsutveksling.api.ConversationStrategy;
import no.difi.meldingsutveksling.nextmove.nhn.DPHMessageOut;
import no.difi.meldingsutveksling.nextmove.nhn.Reciever;
import no.difi.meldingsutveksling.nextmove.nhn.Sender;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class DphConversationStrategyImpl implements ConversationStrategy {


    private RestClient dphClient;

    public DphConversationStrategyImpl(RestClient restClient) {
        this.dphClient = restClient;
    }

    @Override
    @Timed
    public void send(NextMoveOutMessage message) throws NextMoveException {
        DPHMessageOut messageOut = new DPHMessageOut("testMessageId","testConversationId",
            new Sender("testHerid1","testHerId2"), new Reciever("testHerId1","testHerId2"),"fagmelding");

        dphClient.method(HttpMethod.POST).uri("http://localhost:8090/dph/out").body(messageOut);

        System.out.println("DphConversationStrategyImpl.send");
    }
}
