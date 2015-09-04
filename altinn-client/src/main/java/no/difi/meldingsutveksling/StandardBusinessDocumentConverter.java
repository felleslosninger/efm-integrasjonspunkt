package no.difi.meldingsutveksling;

import no.difi.meldingsutveksling.domain.sbdh.ObjectFactory;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class StandardBusinessDocumentConverter {
    public static final int BUFFER_SIZE = 2024;
    static JAXBContext ctx;

    static {
        try {
            ctx = JAXBContext.newInstance(StandardBusinessDocument.class);
        } catch (JAXBException e) {
            throw new RuntimeException("Could not initialize " + StandardBusinessDocumentConverter.class, e);
        }
    }
    public byte[] marshallToBytes(StandardBusinessDocument standardBusinessDocument) {
        Marshaller marshaller;
        try {
            marshaller = ctx.createMarshaller();
        } catch (JAXBException e) {
            throw new RuntimeException("Could not create marshaller for " + StandardBusinessDocument.class, e);
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream(BUFFER_SIZE);
        try {
            ObjectFactory objectFactory = new ObjectFactory();
            marshaller.marshal(objectFactory.createStandardBusinessDocument(standardBusinessDocument), output);
        } catch (JAXBException e) {
            throw new RuntimeException("Could not marshall " + standardBusinessDocument, e);
        }
        return output.toByteArray();
    }

    public StandardBusinessDocument unmarshallFrom(byte[] bytes) {
        Unmarshaller unmarshaller;

        try {
            unmarshaller = ctx.createUnmarshaller();
        } catch (JAXBException e) {
            throw new RuntimeException("Could not create Unmarshaller for " + StandardBusinessDocument.class, e);
        }

        ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[BUFFER_SIZE]);
        StreamSource streamSource = new StreamSource(inputStream);

        StandardBusinessDocument sbd;
        try {
            sbd = unmarshaller.unmarshal(streamSource, StandardBusinessDocument.class).getValue();
        } catch (JAXBException e) {
            throw new RuntimeException("Could not unmarshall to " + StandardBusinessDocument.class, e);
        }

        return sbd;
    }
}
