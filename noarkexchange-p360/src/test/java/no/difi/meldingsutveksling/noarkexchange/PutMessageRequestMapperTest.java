package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.noarkexchange.p360.PutMessageRequestMapper;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import org.junit.Before;
import org.junit.Test;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import static junit.framework.Assert.assertTrue;

public class PutMessageRequestMapperTest {

    private TestData<PutMessageRequestType> testData;

    @Before
    public void setup() throws JAXBException {
        testData = new TestData<>(PutMessageRequestType.class);
    }

    @Test
    public void mapFromEphortePutMessageToP360PutMessage() throws JAXBException, XMLStreamException {
        PutMessageRequestType putMessageRequestType = testData.loadFromClasspath("ephorte/PutMessageMessage.xml");

        PutMessageRequestMapper mapper = new PutMessageRequestMapper();
        no.difi.meldingsutveksling.noarkexchange.p360.schema.PutMessageRequestType p360Request = mapper.mapFrom(putMessageRequestType).getValue();

        assertTrue(!PayloadUtil.isEmpty(p360Request.getPayload()));
        assertTrue(p360Request.getPayload().contains("Melding"));
    }
}