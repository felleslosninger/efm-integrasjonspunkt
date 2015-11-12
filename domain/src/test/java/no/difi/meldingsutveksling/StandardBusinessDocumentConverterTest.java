package no.difi.meldingsutveksling;

import no.difi.meldingsutveksling.domain.sbdh.Document;
import no.difi.meldingsutveksling.domain.sbdh.ObjectFactory;
import org.junit.Test;

public class StandardBusinessDocumentConverterTest {

    @Test
    public void testMarshallToBytes() throws Exception {
        StandardBusinessDocumentConverter converter = new StandardBusinessDocumentConverter();

        Document sbd = new Document();

        sbd.setAny(new ObjectFactory().createScopeInformation("Hello world!"));
        converter.marshallToBytes(sbd);
    }

}