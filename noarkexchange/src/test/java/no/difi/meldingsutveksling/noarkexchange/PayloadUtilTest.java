package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.noarkexchange.schema.EnvelopeType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import org.junit.Before;
import org.junit.Test;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PayloadUtilTest {

    private JAXBContext ctx;
    private Unmarshaller unmarshaller;

    @Before
    public void setup() throws JAXBException {
        ctx = JAXBContext.newInstance(PutMessageRequestType.class, EnvelopeType.class);
        unmarshaller = ctx.createUnmarshaller();
    }

    @Test
    public void isAppReceiptPutMessageFromEphorte() throws Exception {
        PutMessageRequestType value = loadTestdataFromClasspath("ephorte/PutMessageMessage.xml");

        assertFalse(PayloadUtil.isAppReceipt(value.getPayload()));
    }

    @Test
    public void isAppReceiptAppreceiptFrom360() throws JAXBException, XMLStreamException {
        final PutMessageRequestType putMessageRequestType = loadTestdataFromClasspath("p360/PutMessageAppReceipt.xml");

        assertTrue(PayloadUtil.isAppReceipt(putMessageRequestType.getPayload()));
    }

    @Test
    public void isAppReceiptAppreceiptFromEphorte() throws JAXBException, XMLStreamException {
        final PutMessageRequestType putMessageRequestType = loadTestdataFromClasspath("ephorte/PutMessageAppReceipt.xml");

        assertTrue(PayloadUtil.isAppReceipt(putMessageRequestType.getPayload()));
    }

    @Test
    public void isEmptyPayloadFromEphorte() throws JAXBException, XMLStreamException {
        final PutMessageRequestType putMessageRequestType = loadTestdataFromClasspath("ephorte/PutMessageEmptyPayload.xml");

        assertTrue(PayloadUtil.isEmpty(putMessageRequestType.getPayload()));
    }

    public PutMessageRequestType loadTestdataFromClasspath(String fileName) throws JAXBException, XMLStreamException {
        InputStream file = this.getClass().getClassLoader().getResourceAsStream(fileName);

        XMLInputFactory xif = XMLInputFactory.newFactory();
        XMLStreamReader xsr = xif.createXMLStreamReader(file);
        xsr.nextTag(); // Advance to Envelope tag
        xsr.nextTag(); // Advance to Body tag
        xsr.nextTag(); // Advance to getNumberResponse tag
        return unmarshaller.unmarshal(xsr, PutMessageRequestType.class).getValue();
    }
}