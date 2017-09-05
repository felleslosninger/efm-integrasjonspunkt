package no.difi.meldingsutveksling.noarkexchange.receive;

import no.difi.meldingsutveksling.dokumentpakking.xml.Payload;
import no.difi.meldingsutveksling.domain.sbdh.EduDocument;
import no.difi.meldingsutveksling.domain.sbdh.ObjectFactory;
import no.difi.meldingsutveksling.kvittering.xsd.Kvittering;
import org.eclipse.persistence.jaxb.JAXBContextFactory;

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
            ctx = JAXBContextFactory.createContext(new Class[]{EduDocument.class, Payload.class, Kvittering.class}, null);
        } catch (JAXBException e) {
            throw new RuntimeException("Could not initialize " + DocumentConverter.class, e);
        }
    }
    public byte[] marshallToBytes(EduDocument eduDocument) {
        Marshaller marshaller;
        try {
            marshaller = ctx.createMarshaller();
        } catch (JAXBException e) {
            throw new RuntimeException("Could not create marshaller for " + EduDocument.class, e);
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ObjectFactory objectFactory = new ObjectFactory();

        try {
            marshaller.marshal(objectFactory.createStandardBusinessDocument(eduDocument), output);
        } catch (JAXBException e) {
            throw new RuntimeException("Could not marshall " + eduDocument, e);
        }
        return output.toByteArray();
    }

    public EduDocument unmarshallFrom(byte[] bytes) {
        Unmarshaller unmarshaller;

        try {
            unmarshaller = ctx.createUnmarshaller();
        } catch (JAXBException e) {
            throw new RuntimeException("Could not create Unmarshaller for " + EduDocument.class, e);
        }

        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        StreamSource streamSource = new StreamSource(inputStream);

        EduDocument edu;
        try {
            edu = unmarshaller.unmarshal(streamSource, EduDocument.class).getValue();
        } catch (JAXBException e) {
            throw new RuntimeException("Could not unmarshall to " + EduDocument.class, e);
        }

        return edu;
    }
}
