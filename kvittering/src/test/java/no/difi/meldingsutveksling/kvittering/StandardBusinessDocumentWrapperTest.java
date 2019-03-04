package no.difi.meldingsutveksling.kvittering;


import no.difi.meldingsutveksling.IntegrasjonspunktNokkel;
import no.difi.meldingsutveksling.domain.MessageInfo;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import org.junit.Test;
import org.mockito.Mockito;
import org.w3c.dom.Document;

import javax.xml.bind.JAXBException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;

import static no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocumentHeader.KVITTERING_TYPE;
import static org.junit.Assert.assertEquals;


/**
 * @author Glenn Bech
 */
public class StandardBusinessDocumentWrapperTest {

    private static final String AUTHOROTHY = "";
    private static final String SENDER = "974720760";
    private static final String RECEIVER = "974720760";

    @Test
    public void should_convert_to_document_and_back_to_jaxb_representation() throws JAXBException, NoSuchAlgorithmException {

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(512);
        KeyPair kp = kpg.generateKeyPair();

        KeyStore keyStore = Mockito.mock(KeyStore.class);

        IntegrasjonspunktNokkel integrasjonspunktNokkel = Mockito.mock(IntegrasjonspunktNokkel.class);
        Mockito.when(integrasjonspunktNokkel.getKeyPair()).thenReturn(kp);
        Mockito.when(integrasjonspunktNokkel.getKeyStore()).thenReturn(keyStore);
        Mockito.when(integrasjonspunktNokkel.shouldLockProvider()).thenReturn(false);

        StandardBusinessDocument beforeConversion = SBDReceiptFactory.createAapningskvittering(new MessageInfo(RECEIVER, SENDER, "", "", KVITTERING_TYPE), integrasjonspunktNokkel);
        Document xmlDocVersion = DocumentToDocumentConverter.toXMLDocument(beforeConversion);
        StandardBusinessDocument afterConversion = DocumentToDocumentConverter.toDomainDocument(xmlDocVersion);

        assertEquals(beforeConversion.getStandardBusinessDocumentHeader().getSender().iterator().next().getIdentifier().getValue(),
                afterConversion.getStandardBusinessDocumentHeader().getSender().iterator().next().getIdentifier().getValue());
    }

}
