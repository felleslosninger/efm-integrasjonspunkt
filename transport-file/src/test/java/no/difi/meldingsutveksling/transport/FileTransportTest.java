package no.difi.meldingsutveksling.transport;

import no.difi.meldingsutveksling.domain.sbdh.Document;
import no.difi.meldingsutveksling.noarkexchange.schema.receive.StandardBusinessDocument;
import org.modelmapper.ModelMapper;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

/**
 *
 */
public class FileTransportTest {

    public static void main(String[] args) throws JAXBException {
        FileTransport t = new FileTransport();
        JAXBContext c = JAXBContext.newInstance("no.difi.meldingsutveksling.noarkexchange.schema.receive");
        Unmarshaller unm = c.createUnmarshaller();
        StandardBusinessDocument doc = unm.unmarshal(new StreamSource(FileTransport.class.getClassLoader().getResourceAsStream("sbdV2.xml")), StandardBusinessDocument.class).getValue();
        System.out.println(doc.getStandardBusinessDocumentHeader().getReceiver().get(0).getIdentifier().getValue());

        ModelMapper mapper = new ModelMapper();

        Document domainDoc
                = mapper.map(doc, Document.class);

        t.send(null, domainDoc);

    }
}
