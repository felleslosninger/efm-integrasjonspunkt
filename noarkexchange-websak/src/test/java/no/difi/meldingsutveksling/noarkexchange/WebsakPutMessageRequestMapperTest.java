package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.noarkexchange.schema.ObjectFactory;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.websak.PutMessageRequestMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.xml.transform.StringSource;

import javax.xml.bind.*;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class WebsakPutMessageRequestMapperTest {

    private TestData<PutMessageRequestType> testData;

    @BeforeEach
    public void setup() throws JAXBException {
        testData = new TestData<>(PutMessageRequestType.class);
    }

    @Test
    public void emptyPayloadEphorteBug() throws JAXBException, XMLStreamException {
        PutMessageRequestType putMessageRequestType = testData.loadFromClasspath("ephorte/EmptyPayload.xml");

        PutMessageRequestMapper mapper = new PutMessageRequestMapper();

        final JAXBElement<no.difi.meldingsutveksling.noarkexchange.websak.schema.PutMessageRequestType> websakPutMessage = mapper.mapFrom(putMessageRequestType);

        assertNotNull(websakPutMessage.getValue().getPayload());
    }

    @Test()
    public void mapFromEphortePutMessageToWebsakPutMessage() throws JAXBException, XMLStreamException {
        PutMessageRequestType putMessageRequestType = testData.loadFromClasspath("ephorte/PutMessageMessage.xml");

        PutMessageRequestMapper mapper = new PutMessageRequestMapper();
        no.difi.meldingsutveksling.noarkexchange.websak.schema.PutMessageRequestType websakRequest = mapper.mapFrom(putMessageRequestType).getValue();

        assertFalse(PayloadUtil.isEmpty(websakRequest.getPayload()));
        JAXBContext ctx2 = JAXBContext.newInstance(PutMessageRequestType.class);
        Marshaller marshaller = ctx2.createMarshaller();
        StringWriter writer = new StringWriter();
        marshaller.marshal(new ObjectFactory().createPutMessageRequest(putMessageRequestType), writer);
        String xml = writer.toString();
        JAXBContext ctx = JAXBContext.newInstance(no.difi.meldingsutveksling.noarkexchange.websak.schema.PutMessageRequestType.class);
        Unmarshaller unmarshaller = ctx.createUnmarshaller();

        no.difi.meldingsutveksling.noarkexchange.websak.schema.PutMessageRequestType websakResult = unmarshaller.unmarshal(new StringSource(xml), no.difi.meldingsutveksling.noarkexchange.websak.schema.PutMessageRequestType.class).getValue();

        System.out.println(websakResult.getPayload());
    }

    @Test
    public void tryMapper() throws JAXBException, XMLStreamException {
        PutMessageRequestType putMessageRequestType = testData.loadFromClasspath("ephorte/PutMessageMessage.xml");

        JAXBContext ctx2 = JAXBContext.newInstance(PutMessageRequestType.class);
        Marshaller marshaller = ctx2.createMarshaller();

        ByteArrayOutputStream bos = new ByteArrayOutputStream(4096);
        marshaller.marshal(new ObjectFactory().createPutMessageRequest(putMessageRequestType), bos);

        byte[] bytes = bos.toByteArray();
        JAXBContext ctx = JAXBContext.newInstance(no.difi.meldingsutveksling.noarkexchange.websak.schema.PutMessageRequestType.class);

        Unmarshaller unmarshaller = ctx.createUnmarshaller();

        StreamSource source = new StreamSource(new ByteArrayInputStream(bytes));
        no.difi.meldingsutveksling.noarkexchange.websak.schema.PutMessageRequestType value = unmarshaller.unmarshal(source, no.difi.meldingsutveksling.noarkexchange.websak.schema.PutMessageRequestType.class).getValue();
        System.out.println(value.getPayload());


    }
}