package no.difi.meldingsutveksling.noarkexchange.receive;

import no.difi.meldingsutveksling.core.EDUCore;
import no.difi.meldingsutveksling.core.EDUCoreConverter;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class EDUCoreConverterTest {
    @Test
    public void marshallToBytes() throws Exception {
        EDUCore eduCore = new EDUCore();
        final byte[] bytes = EDUCoreConverter.marshallToBytes(eduCore);

        final EDUCore afterConvert = EDUCoreConverter.unmarshallFrom(bytes);
        assertThat(afterConvert, is(eduCore));
    }

    @Test
    public void unmarshallFrom() throws Exception {

    }

}