package no.difi.meldingsutveksling.shipping.ws;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class AltinnReasonTest {
    AltinnReason altinnReason;

    @Before
    public void createAltinnReason() {
        int id = 1;
        String message = "Something went terribly wrong";
        String userid = "200";
        altinnReason = new AltinnReason(id, message, userid);
    }

    @Test
    public void toStringShouldContainReasonId() {
        String actual = altinnReason.toString();

        assertTrue(actual.contains(Integer.toString(altinnReason.getId())));
    }

    @Test
    public void toStringShouldContainMessage() {
        String actual = altinnReason.toString();

        assertTrue(actual.contains(altinnReason.getMessage()));
    }

    @Test
    public void toStringShouldContainUserId() {
        String actual = altinnReason.toString();

        assertTrue(actual.contains(altinnReason.getUserId()));
    }
}