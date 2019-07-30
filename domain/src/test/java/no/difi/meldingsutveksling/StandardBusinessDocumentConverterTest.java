package no.difi.meldingsutveksling;

import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.domain.sbdh.ObjectFactory;
import org.junit.Test;

public class StandardBusinessDocumentConverterTest {

    @Test
    public void testMarshallToBytes() throws Exception {
        StandardBusinessDocumentConverter converter = new StandardBusinessDocumentConverter();

        StandardBusinessDocument sbd = new StandardBusinessDocument();

        sbd.setAny(new ObjectFactory().createScopeInformation("Hello world!"));
        converter.marshallToBytes(sbd);
    }

}