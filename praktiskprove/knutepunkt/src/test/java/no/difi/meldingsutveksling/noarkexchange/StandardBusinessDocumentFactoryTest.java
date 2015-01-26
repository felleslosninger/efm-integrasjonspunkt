package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 */
public class StandardBusinessDocumentFactoryTest {


    @Test
    @Ignore
    public void testConvertFromSBDtoSB() {
        no.difi.meldingsutveksling.noarkexchange.schema.receive.StandardBusinessDocument fromDocument = new no.difi.meldingsutveksling.noarkexchange.schema.receive.StandardBusinessDocument();
        StandardBusinessDocument result = StandardBusinessDocumentFactory.create(fromDocument);
        assertNotNull(result);
    }

}
