package no.difi.meldingsutveksling.noarkexchange.receive;

import no.difi.meldingsutveksling.mxa.schema.domain.Message;
import org.eclipse.persistence.jaxb.JAXBContextFactory;

import javax.xml.bind.*;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class MessageConverter {

    private static final JAXBContext jaxbContext;
    static {
        try {
            jaxbContext = JAXBContextFactory.createContext(new Class[]{Message.class}, null);
        } catch (JAXBException e) {
            throw new RuntimeException("Could not create JAXBContext for " + Message.class);
        }
    }

    public byte[] marshallToBytes(Message message) {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.marshal(new JAXBElement<>(new QName("uri", "local"), Message.class, message), os);
            return os.toByteArray();
        } catch (JAXBException e) {
            throw new RuntimeException("Unable to create unmarshaller for " + Message.class, e);
        }
    }


    public Message unmarshallFrom(byte[] message) {
        final ByteArrayInputStream is = new ByteArrayInputStream(message);
        Unmarshaller unmarshaller;
        try {
            unmarshaller = jaxbContext.createUnmarshaller();
            StreamSource source = new StreamSource(is);
            return unmarshaller.unmarshal(source, Message.class).getValue();
        } catch (JAXBException e) {
            throw new RuntimeException("Unable to create unmarshaller for " + Message.class, e);
        }
    }
}
