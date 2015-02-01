package no.difi.meldingsutveksling.transport;


import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import org.modelmapper.ModelMapper;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.*;

/**
 * Oxalis implementation of the trasnport interface. Uses the oxalis outbound module to transmit the SBD
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
        try (BufferedWriter w = new BufferedWriter(new FileWriter(f))) {
            JAXBContext c = JAXBContext.newInstance(no.difi.meldingsutveksling.noarkexchange.schema.receive.StandardBusinessDocument.class);
            Marshaller m = c.createMarshaller();
            m.marshal(toWrite, f);
        } catch (IOException | JAXBException e) {
            throw new MeldingsUtvekslingRuntimeException("file write error ", e);
        }
    }

    private String createFilename(StandardBusinessDocument document) {
        return document.getStandardBusinessDocumentHeader().getReceiver().get(0).getIdentifier().getValue() + "-" + System.currentTimeMillis();
    }


}
