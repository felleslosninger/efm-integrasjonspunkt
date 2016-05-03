package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.noarkexchange.p360.schema.PutMessageRequestType;
import org.junit.Before;
import org.junit.Test;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

public class PutMessageRequestMapperTest {

    private TestData<PutMessageRequestType> testData;

    @Before
    public void setup() throws JAXBException {
        testData = new TestData<>(PutMessageRequestType.class);
    }

    @Test
    public void mapFromEphortePutMessageToP360PutMessage() throws JAXBException, XMLStreamException {
        testData.loadFromClasspath("ephorte/PutMessageMessage.xml");


    }
}