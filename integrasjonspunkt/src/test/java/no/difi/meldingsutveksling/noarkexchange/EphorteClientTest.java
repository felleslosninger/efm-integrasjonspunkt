package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


@Disabled("Test that can be manually run after starting ephortemock service on port 7778")
public class EphorteClientTest {

    @Test
    public void testSendEduMelding() throws Exception {
        NoarkClientSettings settings = new NoarkClientSettings("http://localhost:7778/ephorte", "", "");
        EphorteClient client = new EphorteClient(settings);

        PutMessageResponseType result = client.sendEduMelding(new PutMessageRequestType());
        assertEquals("Hello world", result.getResult().getType());
    }

    @Test
    public void testCanGetRecieveMessage() {
        NoarkClientSettings settings = new NoarkClientSettings("http://localhost:7778/ephorte", "", "");
        EphorteClient client = new EphorteClient(settings);
        boolean result = client.canRecieveMessage("123");
        assertTrue(result);
    }
}