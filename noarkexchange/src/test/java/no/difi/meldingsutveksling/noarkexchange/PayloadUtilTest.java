package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import org.apache.commons.lang.StringEscapeUtils;
import org.junit.Before;
import org.junit.Test;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import static org.junit.Assert.*;

public class PayloadUtilTest {

    private TestData<PutMessageRequestType> testData;

    @Before
    public void setup() throws JAXBException {
        testData = new TestData<>(PutMessageRequestType.class);
    }

    @Test
    public void getAppreceiptFromP360() throws JAXBException, XMLStreamException {
        PutMessageRequestType value = testData.loadFromClasspath("p360/PutMessageAppReceiptProblem.xml");

        String payload = StringEscapeUtils.unescapeHtml((String) value.getPayload());

        assertNotNull(PayloadUtil.getAppReceiptType(payload));
    }

    @Test
    public void isAppReceiptPutMessageFromEphorte() throws Exception {
        PutMessageRequestType value = testData.loadFromClasspath("ephorte/PutMessageMessage.xml");

        assertFalse(PayloadUtil.isAppReceipt(value.getPayload()));
    }

    @Test
    public void isAppReceiptAppreceiptFrom360() throws JAXBException, XMLStreamException {
        final PutMessageRequestType putMessageRequestType = testData.loadFromClasspath("p360/PutMessageAppReceipt.xml");

        assertTrue(PayloadUtil.isAppReceipt(putMessageRequestType.getPayload()));
    }

    @Test
    public void isAppReceiptAppreceiptFromEphorte() throws JAXBException, XMLStreamException {
        final PutMessageRequestType putMessageRequestType = testData.loadFromClasspath("ephorte/PutMessageAppReceipt.xml");

        assertTrue(PayloadUtil.isAppReceipt(putMessageRequestType.getPayload()));
    }
    
    @Test
    public void isEmptyPayloadFromEphorte() throws JAXBException, XMLStreamException {
        final PutMessageRequestType putMessageRequestType = testData.loadFromClasspath("ephorte/PutMessageEmptyPayload.xml");

        assertTrue(PayloadUtil.isEmpty(putMessageRequestType.getPayload()));
    }
}