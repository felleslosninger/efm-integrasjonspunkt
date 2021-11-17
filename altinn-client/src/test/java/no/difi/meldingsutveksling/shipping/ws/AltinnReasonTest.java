package no.difi.meldingsutveksling.shipping.ws;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class AltinnReasonTest {
    AltinnReason altinnReason;

    @BeforeEach
    public void createAltinnReason() {
        int id = 1;
        String message = "Something went terribly wrong";
        String userid = "200";
        String localizedMessage = "Details why it went wrong";
        altinnReason = new AltinnReason(id, message, userid, localizedMessage);
    }

    @Test
    public void toStringShouldContainReasonId() {
        String actual = altinnReason.toString();

        assertThat(actual.contains(Integer.toString(altinnReason.getId())), is(true));
    }

    @Test
    public void toStringShouldContainMessage() {
        String actual = altinnReason.toString();

        assertThat(actual.contains(altinnReason.getMessage()), is(true));
    }

    @Test
    public void toStringShouldContainUserId() {
        String actual = altinnReason.toString();

        assertThat(actual.contains(altinnReason.getUserId()), is(true));
    }

    @Test
    public void toStringShouldContainLocalizedMessage() {
        String actual = altinnReason.toString();

        assertThat(actual.contains(altinnReason.getLocalized()), is(true));
    }
}