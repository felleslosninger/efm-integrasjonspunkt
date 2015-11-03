package no.difi.meldingsutveksling.shipping.ws;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class AltinnReasonTest {

    @Test
    public void toStringContainsNecessaryDebuggingInformation() throws Exception {
        int id = 1;
        String message = "Something went terribly wrong";
        String userid = "200";
        AltinnReason altinnErrorInfo = new AltinnReason(id, message, userid);

        String actual = altinnErrorInfo.toString();

        assertTrue(actual.contains(Integer.toString(id)));
        assertTrue(actual.contains(message));
        assertTrue(actual.contains(userid));
    }
}