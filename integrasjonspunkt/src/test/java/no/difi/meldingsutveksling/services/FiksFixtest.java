package no.difi.meldingsutveksling.services;

import no.difi.meldingsutveksling.noarkexchange.FiksFix;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FiksFixTest {

    @Test
    public void testShouldReplaceBergnsorgNumberWithKS() {
        assertEquals("910639870", FiksFix.replaceOrgNummberWithKs("910951688"));
    }

    @Test
    public void testShouldNotReplaceBergnsorgNumberWithKS() {
        assertEquals("910075918", "910075918");
    }

}
