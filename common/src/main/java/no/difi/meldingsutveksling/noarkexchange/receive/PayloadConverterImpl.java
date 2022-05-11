package no.difi.meldingsutveksling.noarkexchange.receive;

import org.eclipse.persistence.jaxb.JAXBContextFactory;

import javax.xml.bind.*;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;

public class PayloadConverterImpl<T> implements PayloadConverter<T> {

    private final JAXBContext jaxbContext;
    private Class<T> clazz;
    private String namespaceUri;
    private String localPart;

    public PayloadConverterImpl(Class<T> clazz, String namespaceUri, String localPart) {
        this.clazz = clazz;
        this.namespaceUri = namespaceUri;
        this.localPart = localPart;
        try {
            jaxbContext = JAXBContextFactory.createContext(new Class[]{clazz}, null);
        } catch (JAXBException e) {
            throw new PayloadConverterException("Could not create JAXBContext for " + clazz, e);
        }
    }

    public PayloadConverterImpl(Class<T> clazz) {
        this.clazz = clazz;
        this.namespaceUri = "uri";
        this.localPart = "local";
        try {
            jaxbContext = JAXBContextFactory.createContext(new Class[]{clazz}, null);
        } catch (JAXBException e) {
            throw new PayloadConverterException("Could not create JAXBContext for " + clazz, e);
        }
    }

    @Override
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

    @Override
    public String marshallToString(T message) {
        final StringWriter sw = new StringWriter();
        try {
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.marshal(new JAXBElement<>(new QName(namespaceUri, localPart), clazz, message), sw);
            return sw.toString()
                    .replaceAll(":ns0|ns0:", "")
                    .replaceAll(":ns2|ns2:", "")
                    .replace("<message code", "<message xmlns=\"\" code")
                    .replace("<journpost>", "<journpost xmlns=\"\">")
                    .replace("<noarksak>", "<noarksak xmlns=\"\">");
        } catch (JAXBException e) {
            throw new PayloadConverterException("Unable to create marshaller for " + clazz, e);
        }
    }

    @Override
    public Object marshallToPayload(T message) {
        return marshallToString(message);
    }

    public static class PayloadConverterException extends RuntimeException {
        public PayloadConverterException(String s, Exception e) {
            super(s, e);
        }
    }

}
