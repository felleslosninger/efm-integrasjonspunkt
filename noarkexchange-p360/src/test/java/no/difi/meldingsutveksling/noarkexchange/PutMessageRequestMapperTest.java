package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.noarkexchange.p360.PutMessageRequestMapper;
import no.difi.meldingsutveksling.noarkexchange.schema.ObjectFactory;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.xml.transform.StringSource;

import javax.xml.bind.*;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringWriter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class PutMessageRequestMapperTest {

    private TestData<PutMessageRequestType> testData;

    @Before
    public void setup() throws JAXBException {
        testData = new TestData<>(PutMessageRequestType.class);
    }

    @Test
    public void emptyPayloadEphorteBug() throws JAXBException, XMLStreamException {
        PutMessageRequestType putMessageRequestType = testData.loadFromClasspath("ephorte/EmptyPayload.xml");

        PutMessageRequestMapper mapper = new PutMessageRequestMapper();

        final JAXBElement<no.difi.meldingsutveksling.noarkexchange.p360.schema.PutMessageRequestType> p360PutMessage = mapper.mapFrom(putMessageRequestType);

        assertNotNull(p360PutMessage.getValue().getPayload());
    }

    @Ignore("Work in progress")
    @Test
    public void mapFromEphortePutMessageToP360PutMessage() throws JAXBException, XMLStreamException {
        PutMessageRequestType putMessageRequestType = testData.loadFromClasspath("ephorte/PutMessageMessage.xml");

        PutMessageRequestMapper mapper = new PutMessageRequestMapper();
        no.difi.meldingsutveksling.noarkexchange.p360.schema.PutMessageRequestType p360Request = mapper.mapFrom(putMessageRequestType).getValue();

        assertFalse(PayloadUtil.isEmpty(p360Request.getPayload()));
        //assertTrue(p360Request.getPayload().contains("Melding"));
        JAXBContext ctx2 = JAXBContext.newInstance(PutMessageRequestType.class);
        Marshaller marshaller = ctx2.createMarshaller();
        StringWriter writer = new StringWriter();
        marshaller.marshal(new ObjectFactory().createPutMessageRequest(putMessageRequestType), writer);
        String xml = writer.toString();
        JAXBContext ctx = JAXBContext.newInstance(no.difi.meldingsutveksling.noarkexchange.p360.schema.PutMessageRequestType.class);
        Unmarshaller unmarshaller = ctx.createUnmarshaller();
        //System.out.println(xml);

        no.difi.meldingsutveksling.noarkexchange.p360.schema.PutMessageRequestType p360result = unmarshaller.unmarshal(new StringSource(xml), no.difi.meldingsutveksling.noarkexchange.p360.schema.PutMessageRequestType.class).getValue();

        System.out.println(p360result.getPayload());

    }

    @Test
    public void tryMapper() throws JAXBException, XMLStreamException {
        PutMessageRequestType putMessageRequestType = testData.loadFromClasspath("ephorte/PutMessageMessage.xml");

        JAXBContext ctx2 = JAXBContext.newInstance(PutMessageRequestType.class);
        Marshaller marshaller = ctx2.createMarshaller();

        ByteArrayOutputStream bos = new ByteArrayOutputStream(4096);
        marshaller.marshal(new ObjectFactory().createPutMessageRequest(putMessageRequestType), bos);

        byte[] bytes = bos.toByteArray();
        JAXBContext ctx = JAXBContext.newInstance(no.difi.meldingsutveksling.noarkexchange.p360.schema.PutMessageRequestType.class);

        Unmarshaller unmarshaller = ctx.createUnmarshaller();

        StreamSource source = new StreamSource(new ByteArrayInputStream(bytes));
        no.difi.meldingsutveksling.noarkexchange.p360.schema.PutMessageRequestType value = unmarshaller.unmarshal(source, no.difi.meldingsutveksling.noarkexchange.p360.schema.PutMessageRequestType.class).getValue();
        System.out.println(value.getPayload());


    }
}