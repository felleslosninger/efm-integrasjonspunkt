package no.difi.meldingsutveksling.kvittering;


import no.difi.meldingsutveksling.kvittering.xsd.Kvittering;
import no.difi.meldingsutveksling.kvittering.xsd.ObjectFactory;
import no.difi.meldingsutveksling.noarkexchange.schema.receive.Partner;
import no.difi.meldingsutveksling.noarkexchange.schema.receive.PartnerIdentification;
import no.difi.meldingsutveksling.noarkexchange.schema.receive.StandardBusinessDocument;
import no.difi.meldingsutveksling.noarkexchange.schema.receive.StandardBusinessDocumentHeader;
import org.junit.Test;
import org.w3c.dom.Document;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import static org.junit.Assert.assertEquals;

/**
 * @author Glenn Bech
 */
public class StandardBusinessDocumentWrapperTest {

    private static final String AUTHOROTHY = "";

    @Test
    public void should_convert_to_document_and_back_to_jaxb_representation() throws JAXBException {

        StandardBusinessDocument document = new StandardBusinessDocument();
        StandardBusinessDocumentHeader header = new StandardBusinessDocumentHeader();

        Partner sender = new Partner();
        PartnerIdentification piSender = new PartnerIdentification();
        piSender.setAuthority(AUTHOROTHY);
        piSender.setValue("974720760");
        sender.setIdentifier(piSender);

        Partner receiver = new Partner();
        PartnerIdentification piReceiver = new PartnerIdentification();
        piReceiver.setAuthority(AUTHOROTHY);
        piReceiver.setValue("974720760");
        receiver.setIdentifier(piReceiver);
        header.getSender().add(0, sender);
        header.getReceiver().add(0, receiver);
        document.setStandardBusinessDocumentHeader(header);
        Kvittering k = KvitteringFactory.createAapningskvittering();
        JAXBElement<Kvittering> kvittering = new ObjectFactory().createKvittering(k);
        document.setAny(kvittering);

        DocumentToDocumentConverter wrapper = new DocumentToDocumentConverter(document);
        Document documentVersionOfTheObject = wrapper.toDocument();

        DocumentToDocumentConverter anotherWrapper = new DocumentToDocumentConverter(documentVersionOfTheObject);
        anotherWrapper.getStandardBusinessDocument();

        assertEquals(wrapper.getStandardBusinessDocument().getStandardBusinessDocumentHeader().getSender().get(0).getIdentifier().getValue(),
                anotherWrapper.getStandardBusinessDocument().getStandardBusinessDocumentHeader().getSender().get(0).getIdentifier().getValue());
    }

}
