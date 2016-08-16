package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Ignore("Test that can be manually run after starting ephortemock service on port 7778")
public class EphorteClientTest {

    @Test
    public void testSendEduMelding() throws Exception {
        NoarkClientSettings settings = new NoarkClientSettings("http://localhost:7778/ephorte", "", "");
        EphorteClient client = new EphorteClient(settings);

        PutMessageResponseType result = client.sendEduMelding(new PutMessageRequestType());
        assertEquals(result.getResult().getType(), "Hello world");
    }

    @Test
    public void testCanGetRecieveMessage() {
        NoarkClientSettings settings = new NoarkClientSettings("http://localhost:7778/ephorte", "", "");
        EphorteClient client = new EphorteClient(settings);
        boolean result = client.canRecieveMessage("123");
        assertTrue(result);
    }
}