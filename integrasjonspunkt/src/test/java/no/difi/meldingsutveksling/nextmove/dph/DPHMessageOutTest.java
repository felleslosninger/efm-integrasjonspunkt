package no.difi.meldingsutveksling.nextmove.dph;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.difi.meldingsutveksling.nextmove.nhn.DPHMessageOut;
import no.difi.meldingsutveksling.nextmove.nhn.Reciever;
import no.difi.meldingsutveksling.nextmove.nhn.Sender;
import org.junit.jupiter.api.Test;


public class DPHMessageOutTest {

    @Test
    public void testDphSerialization() throws Exception {
        DPHMessageOut testMessageOut = new DPHMessageOut("testMessageId","testConversationId", new Sender("testHerid1","testHerid2"), new Reciever("testHerid1","testHerid2"),"testfagmelding");

        ObjectMapper mapper = new ObjectMapper();
        var json = mapper.writeValueAsString(testMessageOut);
        System.out.println(json);


    }

}
