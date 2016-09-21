package no.difi.meldingsutveksling.noarkexchange.receive;

import javax.xml.bind.*;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringWriter;

public class PayloadConverter<T> {

    private final JAXBContext jaxbContext;
    private Class<T> clazz;

    public PayloadConverter(Class<T> clazz) {
        this.clazz = clazz;

        try {
            jaxbContext = JAXBContext.newInstance(clazz);
        } catch (JAXBException e) {
            throw new RuntimeException("Could not create JAXBContext for " + clazz);
        }
    }

    public byte[] marshallToBytes(T message) {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.marshal(new JAXBElement<>(new QName("uri", "local"), clazz, message), os);
            return os.toByteArray();
        } catch (JAXBException e) {
            throw new RuntimeException("Unable to create marshaller for " + clazz, e);
        }
    }

    public String marshallToString(T message) {
        final StringWriter sw = new StringWriter();
        try {
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.marshal(new JAXBElement<>(new QName("uri", "local"), clazz, message), sw);
            return sw.toString();
        } catch (JAXBException e) {
            throw new RuntimeException("Unable to create marshaller for " + clazz, e);
        }
    }

    public T unmarshallFrom(byte[] message) {
        final ByteArrayInputStream is = new ByteArrayInputStream(message);
        Unmarshaller unmarshaller;
        try {
            unmarshaller = jaxbContext.createUnmarshaller();
            StreamSource source = new StreamSource(is);
            return unmarshaller.unmarshal(source, clazz).getValue();
        } catch (JAXBException e) {
            throw new RuntimeException("Unable to create unmarshaller for " + clazz, e);
        }
    }
}
