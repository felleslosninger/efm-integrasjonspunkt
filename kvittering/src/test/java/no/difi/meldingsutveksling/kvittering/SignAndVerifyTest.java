package no.difi.meldingsutveksling.kvittering;


import no.difi.meldingsutveksling.domain.sbdh.Document;
import org.junit.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.assertTrue;

/**
 * @author Glenn Bech
 */

public class SignAndVerifyTest {

    @Test
    public void should_be_able_to_sign_and_verify_the_same_document() throws Exception {

        Handler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.FINEST);
        Logger.getAnonymousLogger().addHandler(consoleHandler);

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(512);
        KeyPair kp = kpg.generateKeyPair();

        Document doc = KvitteringFactory.createAapningskvittering("111111111", "222222222", "123", "456", kp);
        final org.w3c.dom.Document xmlDocument = DocumentToDocumentConverter.toXMLDocument(doc);
        org.w3c.dom.Document signedDomDocument = DocumentSigner.sign(xmlDocument, kp);
        assertTrue(DocumentValidator.validate(signedDomDocument));
    }

}
