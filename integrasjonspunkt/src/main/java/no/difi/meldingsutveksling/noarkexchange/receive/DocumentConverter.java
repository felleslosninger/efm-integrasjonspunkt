package no.difi.meldingsutveksling.noarkexchange.receive;

import no.difi.meldingsutveksling.dokumentpakking.xml.Payload;
import no.difi.meldingsutveksling.domain.sbdh.Document;
import no.difi.meldingsutveksling.domain.sbdh.ObjectFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class DocumentConverter {
    static JAXBContext ctx;

    static {
        try {
            ctx = JAXBContext.newInstance(Document.class, Payload.class);
        } catch (JAXBException e) {
            throw new RuntimeException("Could not initialize " + DocumentConverter.class, e);
        }
    }
    public byte[] marshallToBytes(Document document) {
        Marshaller marshaller;
        try {
            marshaller = ctx.createMarshaller();
        } catch (JAXBException e) {
            throw new RuntimeException("Could not create marshaller for " + Document.class, e);
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ObjectFactory objectFactory = new ObjectFactory();

        try {
            marshaller.marshal(objectFactory.createStandardBusinessDocument(document), output);
        } catch (JAXBException e) {
            throw new RuntimeException("Could not marshall " + document, e);
        }
        return output.toByteArray();
    }

    public Document unmarshallFrom(byte[] bytes) {
        Unmarshaller unmarshaller;

        try {
            unmarshaller = ctx.createUnmarshaller();
        } catch (JAXBException e) {
            throw new RuntimeException("Could not create Unmarshaller for " + Document.class, e);
        }

        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        StreamSource streamSource = new StreamSource(inputStream);

        Document sbd;
        try {
            sbd = unmarshaller.unmarshal(streamSource, Document.class).getValue();
        } catch (JAXBException e) {
            throw new RuntimeException("Could not unmarshall to " + Document.class, e);
        }

        return sbd;
    }
}
