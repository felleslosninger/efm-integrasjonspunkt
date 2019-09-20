package no.difi.meldingsutveksling.transport;

import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.Payload;
import no.difi.meldingsutveksling.domain.sbdh.ObjectFactory;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.kvittering.xsd.Kvittering;
import org.eclipse.persistence.jaxb.JAXBContextFactory;
import org.springframework.context.ApplicationContext;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * Oxalis implementation of the trasnport interface. Uses the oxalis outbound module to transmit the SBD
 *
 * @author Glenn Bech
 */
public class FileTransport implements Transport {

    @Override
    public void send(ApplicationContext context, StandardBusinessDocument sbd) {
        String fileName = createFilename();

        File f = new File(fileName);
        try {
            JAXBContext jaxbContext = JAXBContextFactory.createContext(new Class[]{StandardBusinessDocument.class, Payload.class, Kvittering.class}, null);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            jaxbMarshaller.marshal(new ObjectFactory().createStandardBusinessDocument(sbd), new FileOutputStream(f));
        } catch (JAXBException e) {
            throw new MeldingsUtvekslingRuntimeException("file write error ", e);
        } catch (FileNotFoundException e) {
            throw new MeldingsUtvekslingRuntimeException();
        }
    }

    /**
     * Not currently in use.
     *
     * @param context
     * @param sbd     An sbd with a payload consisting of metadata only
     * @param is      InputStream pointing to the encrypted ASiC package
     */
    @Override
    public void send(ApplicationContext context, StandardBusinessDocument sbd, InputStream is) {
        // Not currently in use
    }

    private String createFilename() {
        return System.currentTimeMillis() + ".xml";
    }
}
