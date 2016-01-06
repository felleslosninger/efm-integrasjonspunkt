package no.difi.meldingsutveksling.kvittering;

import no.difi.meldingsutveksling.dokumentpakking.kvit.Kvittering;
import no.difi.meldingsutveksling.noarkexchange.schema.receive.StandardBusinessDocument;
import no.difi.meldingsutveksling.noarkexchange.schema.receive.StandardBusinessDocumentHeader;
import org.junit.Test;
import org.w3c.dom.Document;

import javax.xml.bind.JAXBElement;
import java.security.KeyPair;
import java.security.KeyPairGenerator;

import static org.junit.Assert.assertTrue;

/**
 * @author Glenn Bech
 */

public class SignAndVerifyTest {

    @Test
    public void should_be_able_to_sign_and_verify_the_same_document() throws Exception {
        StandardBusinessDocument doc = createStandardBusinessDocument();

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("DSA");
        kpg.initialize(512);
        KeyPair kp = kpg.generateKeyPair();

        StandardBusinessDocumentWrapper wrapper = new StandardBusinessDocumentWrapper(doc);
        Document signedDomDocument = DocumentSigner.sign(wrapper.toDocument(), kp);
        assertTrue(DocumentValidator.validate(signedDomDocument));
    }

    private StandardBusinessDocument createStandardBusinessDocument() {
        StandardBusinessDocument doc = new StandardBusinessDocument();
        StandardBusinessDocumentHeader header = new StandardBusinessDocumentHeader();
        doc.setStandardBusinessDocumentHeader(header);

        Kvittering k = KvitteringFactory.createAapningskvittering();
        JAXBElement<Kvittering> kvitteringJAXBElement =
                new no.difi.meldingsutveksling.dokumentpakking.kvit.ObjectFactory().createKvittering(k);
        doc.setAny(kvitteringJAXBElement);
        return doc;
    }


}
