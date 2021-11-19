package no.difi.meldingsutveksling.noarkexchange;

import net.logstash.logback.encoder.org.apache.commons.lang.StringEscapeUtils;
import no.difi.meldingsutveksling.noarkexchange.schema.AppReceiptType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import static org.junit.jupiter.api.Assertions.*;


public class PayloadUtilTest {

    private TestData<PutMessageRequestType> testData;

    @BeforeEach
    public void setup() throws JAXBException {
        testData = new TestData<>(PutMessageRequestType.class);
    }

    @Test
    public void appReceiptMedFeilmeldingIsNotOkType() throws JAXBException, XMLStreamException {
        PutMessageRequestType value = testData.loadFromClasspath("p360/PutMessageAppReceiptFeilmelding.xml");

        final AppReceiptType appReceiptType = PayloadUtil.getAppReceiptType(value.getPayload());
        assertNotEquals(appReceiptType.getType(), "OK");
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
        final PutMessageRequestType putMessageRequestType = testData.loadFromClasspath("p360/OKPutMessageAppReceipt.xml");

        assertTrue(PayloadUtil.isAppReceipt(putMessageRequestType.getPayload()));
    }

    @Test
    public void getOKAppReceiptTypeFromP360() throws JAXBException, XMLStreamException {
        final PutMessageRequestType putMessageRequestType = testData.loadFromClasspath("p360/OKPutMessageAppReceipt.xml");

        final AppReceiptType appReceiptType = PayloadUtil.getAppReceiptType(putMessageRequestType.getPayload());
        assertNotNull(appReceiptType);
        assertTrue(appReceiptType.getType().equals("OK"));
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