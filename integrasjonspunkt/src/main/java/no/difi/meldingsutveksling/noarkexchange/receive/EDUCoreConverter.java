package no.difi.meldingsutveksling.noarkexchange.receive;

import no.difi.meldingsutveksling.core.EDUCore;

import javax.xml.bind.*;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class EDUCoreConverter {

    private static final JAXBContext jaxbContext;
    static {
        try {
            jaxbContext = JAXBContext.newInstance(EDUCore.class);
        } catch (JAXBException e) {
            throw new RuntimeException("Could not create JAXBContext for " + EDUCore.class);
        }
    }

    public byte[] marshallToBytes(EDUCore message) {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.marshal(new JAXBElement<>(new QName("uri", "local"), EDUCore.class, message), os);
            return os.toByteArray();
        } catch (JAXBException e) {
            throw new RuntimeException("Unable to create unmarshaller for " + EDUCore.class, e);
        }
    }


    public EDUCore unmarshallFrom(byte[] message) {
        final ByteArrayInputStream is = new ByteArrayInputStream(message);
        Unmarshaller unmarshaller;
        try {
            unmarshaller = jaxbContext.createUnmarshaller();
            StreamSource source = new StreamSource(is);
            return unmarshaller.unmarshal(source, EDUCore.class).getValue();
        } catch (JAXBException e) {
            throw new RuntimeException("Unable to create unmarshaller for " + EDUCore.class, e);
        }
    }
}
