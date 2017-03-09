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
    private String namespaceUri;
    private String localPart;

    public PayloadConverter(Class<T> clazz, String namespaceUri, String localPart) {
        this.clazz = clazz;
        this.namespaceUri = namespaceUri;
        this.localPart = localPart;
        try {
            jaxbContext = JAXBContext.newInstance(clazz);
        } catch (JAXBException e) {
            throw new PayloadConverterException("Could not create JAXBContext for " + clazz, e);
        }
    }

    public PayloadConverter(Class<T> clazz) {
        this.clazz = clazz;
        this.namespaceUri = "uri";
        this.localPart = "local";
        try {
            jaxbContext = JAXBContext.newInstance(clazz);
        } catch (JAXBException e) {
            throw new PayloadConverterException("Could not create JAXBContext for " + clazz, e);
        }
    }

    public byte[] marshallToBytes(T message) {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.marshal(new JAXBElement<>(new QName(namespaceUri, localPart), clazz, message), os);
            return os.toByteArray();
        } catch (JAXBException e) {
            throw new PayloadConverterException("Unable to create marshaller for " + clazz, e);
        }
    }

    public String marshallToString(T message) {
        final StringWriter sw = new StringWriter();
        try {
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.marshal(new JAXBElement<>(new QName(namespaceUri, localPart), clazz, message), sw);
            return sw.toString();
        } catch (JAXBException e) {
            throw new PayloadConverterException("Unable to create marshaller for " + clazz, e);
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
            throw new PayloadConverterException("Unable to create unmarshaller for " + clazz, e);
        }
    }

    public static class PayloadConverterException extends RuntimeException {
        public PayloadConverterException(String s, Exception e) {
            super(s, e);
        }
    }

}
