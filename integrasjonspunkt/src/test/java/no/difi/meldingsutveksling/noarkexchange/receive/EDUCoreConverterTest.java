package no.difi.meldingsutveksling.noarkexchange.receive;

import no.difi.meldingsutveksling.core.EDUCore;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class EDUCoreConverterTest {
    @Test
    public void marshallToBytes() throws Exception {
        EDUCoreConverter converter = new EDUCoreConverter();
//        PutMessageObjectMother.createMessageRequestType("123");
        EDUCore eduCore = new EDUCore();
        final byte[] bytes = converter.marshallToBytes(eduCore);

        final EDUCore afterConvert = converter.unmarshallFrom(bytes);
        assertThat(afterConvert, is(eduCore));
    }

    @Test
    public void unmarshallFrom() throws Exception {

    }

}