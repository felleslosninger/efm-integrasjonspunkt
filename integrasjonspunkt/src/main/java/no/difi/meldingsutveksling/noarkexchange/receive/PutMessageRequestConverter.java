package no.difi.meldingsutveksling.noarkexchange.receive;

import no.difi.meldingsutveksling.domain.sbdh.Document;
import no.difi.meldingsutveksling.kvittering.xsd.Kvittering;
import no.difi.meldingsutveksling.noarkexchange.schema.AppReceiptType;
import no.difi.meldingsutveksling.noarkexchange.schema.ObjectFactory;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;

import javax.xml.bind.*;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class PutMessageRequestConverter {

    private static final JAXBContext jaxbContext;
    static {
        try {
            jaxbContext = JAXBContext.newInstance("no.difi.meldingsutveksling.kvittering.xsd:no.difi.meldingsutveksling.noarkexchange.schema");
        } catch (JAXBException e) {
            throw new RuntimeException("Could not create JAXBContext for " + PutMessageRequestType.class);
        }
    }

    public byte[] marshallToBytes(PutMessageRequestType request) {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.marshal(new JAXBElement<>(new QName("uri", "local"), PutMessageRequestType.class, request), os);
            return os.toByteArray();
        } catch (JAXBException e) {
            throw new RuntimeException("Unable to create unmarshaller for " + PutMessageRequestType.class, e);
        }
    }


    public PutMessageRequestType unmarshallFrom(byte[] message) {
        final ByteArrayInputStream is = new ByteArrayInputStream(message);
        Unmarshaller unmarshaller;
        try {
            unmarshaller = jaxbContext.createUnmarshaller();
            StreamSource source = new StreamSource(is);
            return unmarshaller.unmarshal(source, PutMessageRequestType.class).getValue();
        } catch (JAXBException e) {
            throw new RuntimeException("Unable to create unmarshaller for " + PutMessageRequestType.class, e);
        }
    }
}
