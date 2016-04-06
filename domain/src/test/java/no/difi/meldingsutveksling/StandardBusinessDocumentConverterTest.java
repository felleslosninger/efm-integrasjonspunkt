package no.difi.meldingsutveksling;

import no.difi.meldingsutveksling.domain.sbdh.EduDocument;
import no.difi.meldingsutveksling.domain.sbdh.ObjectFactory;
import org.junit.Test;

public class StandardBusinessDocumentConverterTest {

    @Test
    public void testMarshallToBytes() throws Exception {
        StandardBusinessDocumentConverter converter = new StandardBusinessDocumentConverter();

        EduDocument sbd = new EduDocument();

        sbd.setAny(new ObjectFactory().createScopeInformation("Hello world!"));
        converter.marshallToBytes(sbd);
    }

}