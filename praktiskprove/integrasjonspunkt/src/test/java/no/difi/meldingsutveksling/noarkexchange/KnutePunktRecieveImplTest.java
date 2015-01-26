package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
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
        File file = new File(getClass().getClassLoader().getResource("sbdV4.xml").getFile());
        JAXBContext jaxbContext = null;
        try {
            jaxbContext = JAXBContext.newInstance(StandardBusinessDocument.class);
        } catch (JAXBException e) {

            throw new MeldingsUtvekslingRuntimeException(e);
        }
        Unmarshaller unmarshaller = null;
        try {
            unmarshaller = jaxbContext.createUnmarshaller();
        } catch (JAXBException e) {

            throw new MeldingsUtvekslingRuntimeException(e);
        }
        StandardBusinessDocument standardBusinessDocument =null;
        try {
            JAXBElement<StandardBusinessDocument> element = (JAXBElement<StandardBusinessDocument>) unmarshaller.unmarshal(file);
            standardBusinessDocument = element.getValue();
        } catch (JAXBException e) {
            throw new MeldingsUtvekslingRuntimeException(e);
        }

            knutePunktReceive.receive(standardBusinessDocument);

    }


}
