package no.difi.meldingsutveksling.shipping.ws;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AltinnReasonTest {
    AltinnReason altinnReason;

    @Before
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

        assertThat(actual.contains(Integer.toString(altinnReason.getId()))).isTrue();
    }

    @Test
    public void toStringShouldContainMessage() {
        String actual = altinnReason.toString();

        assertThat(actual.contains(altinnReason.getMessage())).isTrue();
    }

    @Test
    public void toStringShouldContainUserId() {
        String actual = altinnReason.toString();

        assertThat(actual.contains(altinnReason.getUserId())).isTrue();
    }

    @Test
    public void toStringShouldContainLocalizedMessage() {
        String actual = altinnReason.toString();

        assertThat(actual.contains(altinnReason.getLocalized())).isTrue();
    }
}