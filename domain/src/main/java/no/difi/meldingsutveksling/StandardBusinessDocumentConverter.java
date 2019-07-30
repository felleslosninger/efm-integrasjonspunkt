package no.difi.meldingsutveksling;

import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.domain.sbdh.ObjectFactory;
import org.eclipse.persistence.jaxb.JAXBContextFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class StandardBusinessDocumentConverter {
    static JAXBContext ctx;

    static {
        try {
            ctx = JAXBContextFactory.createContext(new Class[]{StandardBusinessDocument.class}, null);
        } catch (JAXBException e) {
            throw new RuntimeException("Could not initialize " + StandardBusinessDocumentConverter.class, e);
        }
    }
    public byte[] marshallToBytes(StandardBusinessDocument sbd) {
        Marshaller marshaller;
        try {
            marshaller = ctx.createMarshaller();
        } catch (JAXBException e) {
            throw new RuntimeException("Could not create marshaller for " + StandardBusinessDocument.class, e);
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ObjectFactory objectFactory = new ObjectFactory();

        try {
            marshaller.marshal(objectFactory.createStandardBusinessDocument(sbd), output);
        } catch (JAXBException e) {
            throw new RuntimeException("Could not marshall " + sbd, e);
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

        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
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
