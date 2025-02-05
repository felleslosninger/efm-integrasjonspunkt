package no.difi.meldingsutveksling.manifest.xml;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.persistence.jaxb.JAXBContextFactory;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import java.io.OutputStream;

@Slf4j
@UtilityClass
public final class MarshalManifest {

    private static final JAXBContext jaxbContext;

    static {
        try {
            jaxbContext = JAXBContextFactory.createContext(new Class[]{Manifest.class}, null);
        } catch (JAXBException e) {
            throw new IllegalStateException("Could not create JAXBContext", e);
        }
    }

    public static void marshal(Manifest doc, OutputStream os) {
        try {
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            jaxbMarshaller.marshal(doc, os);
        } catch (JAXBException e) {
            log.error("Marshalling failed", e);
        }
    }
}
