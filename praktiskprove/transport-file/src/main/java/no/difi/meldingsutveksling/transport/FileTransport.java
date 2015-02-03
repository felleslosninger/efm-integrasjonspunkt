package no.difi.meldingsutveksling.transport;


import no.difi.meldingsutveksling.dokumentpakking.kvit.Kvittering;
import no.difi.meldingsutveksling.dokumentpakking.xml.Payload;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.sbdh.ObjectFactory;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import org.modelmapper.ModelMapper;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

/**
 * Oxalis implementation of the trasnport interface. Uses the oxalis outbound
 * module to transmit the SBD
 *
 * @author Glenn Bech
 */
public class FileTransport implements Transport {

    @Override
    public void send(StandardBusinessDocument document) {
        String fileName = createFilename(document);
        ModelMapper mapper = new ModelMapper();

        no.difi.meldingsutveksling.noarkexchange.schema.receive.StandardBusinessDocument toWrite =
                mapper.map(document, no.difi.meldingsutveksling.noarkexchange.schema.receive.StandardBusinessDocument.class);

        File f = new File(fileName);
        try {
           JAXBContext jaxbContext = JAXBContext.newInstance(new Class[] {StandardBusinessDocument.class, Payload.class, Kvittering.class});
           Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
           jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
           jaxbMarshaller.marshal(new ObjectFactory().createStandardBusinessDocument(document), new FileOutputStream(f));
            JAXBElement<no.difi.meldingsutveksling.noarkexchange.schema.receive.StandardBusinessDocument> element =
                    new JAXBElement<>(new QName("ns"),
                            no.difi.meldingsutveksling.noarkexchange.schema.receive.StandardBusinessDocument.class, toWrite);
        } catch (JAXBException e) {
            throw new MeldingsUtvekslingRuntimeException("file write error ", e);
        } catch (FileNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    private String createFilename(StandardBusinessDocument document) {
        return document.getStandardBusinessDocumentHeader().getReceiver().get(0).getIdentifier().getValue() + "-" + System.currentTimeMillis();
    }

}
