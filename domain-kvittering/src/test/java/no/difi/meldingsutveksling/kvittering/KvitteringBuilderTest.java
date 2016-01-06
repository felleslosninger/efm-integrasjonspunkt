package no.difi.meldingsutveksling.kvittering;

import no.difi.meldingsutveksling.dokumentpakking.kvit.Kvittering;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Glenn Bech
 */
public class KvitteringBuilderTest {

    @Test
    public void testLeveringsKvittering() {
        Kvittering k = KvitteringFactory.createAapningskvittering();
        assertNotNull(k.getAapning());
        assertNull(k.getLevering());
        assertNull(k.getVarslingfeilet());
        assertNotNull(k.getTidspunkt());
    }

    @Test
    public void testAapningskvittering() {
        Kvittering k = KvitteringFactory.createLeveringsKvittering();
        assertNull(k.getAapning());
        assertNotNull(k.getLevering());
        assertNull(k.getVarslingfeilet());
        assertNotNull(k.getTidspunkt());
    }

}
