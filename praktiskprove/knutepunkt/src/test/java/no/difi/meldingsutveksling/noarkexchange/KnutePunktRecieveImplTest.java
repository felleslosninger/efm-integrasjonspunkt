package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.noarkexchange.schema.receive.StandardBusinessDocument;
import org.junit.Ignore;
import org.junit.Test;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;

/**
 * Created by kubkaray on 27.11.2014.
 */
public class KnutePunktRecieveImplTest {
    @Ignore
    @Test
    public void recieverTesting() {
        KnutePunktReceiveImpl knutePunktReceive = new KnutePunktReceiveImpl();
        File file = new File(getClass().getClassLoader().getResource("sbdUt.xml").getFile());
        JAXBContext jaxbContext = null;
        try {
            jaxbContext = JAXBContext.newInstance(StandardBusinessDocument.class);
        } catch (JAXBException e) {
            e.printStackTrace();
        }
        Unmarshaller unmarshaller = null;
        try {
            unmarshaller = jaxbContext.createUnmarshaller();
        } catch (JAXBException e) {
            e.printStackTrace();
        }
        StandardBusinessDocument standardBusinessDocument =null;
        try {
            JAXBElement<StandardBusinessDocument> element = (JAXBElement<StandardBusinessDocument>) unmarshaller.unmarshal(file);
            standardBusinessDocument = element.getValue();
        } catch (JAXBException e) {
            e.printStackTrace();
        }

            knutePunktReceive.receive(standardBusinessDocument);

    }


}
