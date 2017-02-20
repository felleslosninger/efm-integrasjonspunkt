package no.difi.meldingsutveksling.ks

import no.difi.meldingsutveksling.ks.svarinn.SvarInnClient
import org.junit.Test
import no.difi.meldingsutveksling.ks.svarinn.Forsendelse as Forsend

public class SvarInnClientTest {
    @Test
    public void checkForNewMessages() {
        SvarInnClient client = new SvarInnClient();
        final List<Forsend> forsendelses = client.checkForNewMessages();
        System.out.println(forsendelses);
    }

}