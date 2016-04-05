package no.difi.meldingsutveksling.kvittering;


import no.difi.meldingsutveksling.domain.MessageInfo;
import no.difi.meldingsutveksling.domain.sbdh.EduDocument;
import org.junit.Test;
import org.w3c.dom.Document;

import javax.xml.bind.JAXBException;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

import static org.junit.Assert.assertEquals;

/**
 * @author Glenn Bech
 */
public class StandardBusinessDocumentWrapperTest {

    private static final String AUTHOROTHY = "";
    public static final String SENDER = "974720760";
    public static final String RECEIVER = "974720760";

    @Test
    public void should_convert_to_document_and_back_to_jaxb_representation() throws JAXBException, NoSuchAlgorithmException {

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(512);
        KeyPair kp = kpg.generateKeyPair();

        EduDocument beforeConversion = KvitteringFactory.createAapningskvittering(new MessageInfo(RECEIVER, SENDER, "", ""), kp);
        Document xmlDocVersion = DocumentToDocumentConverter.toXMLDocument(beforeConversion);
        EduDocument afterConversion = DocumentToDocumentConverter.toDomainDocument(xmlDocVersion);

        assertEquals(beforeConversion.getStandardBusinessDocumentHeader().getSender().get(0).getIdentifier().getValue(),
                afterConversion.getStandardBusinessDocumentHeader().getSender().get(0).getIdentifier().getValue());
    }

}
