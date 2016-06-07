package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.domain.sbdh.EduDocument;
import org.junit.Test;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import static junit.framework.Assert.assertNotNull;

/**
 */
public class StandardBusinessDocumentFactoryTest {

    @Test
    public void testConvertFromSBDtoSB() throws JAXBException {
        JAXBElement<no.difi.meldingsutveksling.noarkexchange.schema.receive.StandardBusinessDocument> fromDocument;
        JAXBContext ctx = JAXBContext.newInstance(no.difi.meldingsutveksling.noarkexchange.schema.receive.StandardBusinessDocument.class);
        Unmarshaller unmarshaller = ctx.createUnmarshaller();
        fromDocument = unmarshaller.unmarshal(new StreamSource(getClass().getClassLoader().getResourceAsStream("sample_dbd_document.xml")), no.difi.meldingsutveksling.noarkexchange.schema.receive.StandardBusinessDocument.class);
        EduDocument result = StandardBusinessDocumentFactory.create(fromDocument.getValue());
        assertNotNull(result);
    }

}
