package no.difi.meldingsutveksling.kvittering;


import no.difi.meldingsutveksling.DocumentType;
import no.difi.meldingsutveksling.domain.MessageInfo;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.move.common.cert.KeystoreHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.w3c.dom.Document;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Glenn Bech
 */

@RunWith(MockitoJUnitRunner.class)
public class StandardBusinessDocumentWrapperTest {

    private static final String SENDER = "974720760";
    private static final String RECEIVER = "974720760";

    @InjectMocks private SBDReceiptFactory sbdReceiptFactory;

    @Test
    public void should_convert_to_document_and_back_to_jaxb_representation() throws NoSuchAlgorithmException {

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(512);
        KeyPair kp = kpg.generateKeyPair();

        KeystoreHelper keystoreHelper = Mockito.mock(KeystoreHelper.class);
        Mockito.when(keystoreHelper.getKeyPair()).thenReturn(kp);
        Mockito.when(keystoreHelper.shouldLockProvider()).thenReturn(false);

        MessageInfo mi = new MessageInfo(DocumentType.BESTEDU_KVITTERING.getType(), SENDER, RECEIVER, "", "", "");
        StandardBusinessDocument beforeConversion = sbdReceiptFactory.createAapningskvittering(mi, keystoreHelper);
        Document xmlDocVersion = DocumentToDocumentConverter.toXMLDocument(beforeConversion);
        StandardBusinessDocument afterConversion = DocumentToDocumentConverter.toDomainDocument(xmlDocVersion);

        assertThat(afterConversion.getStandardBusinessDocumentHeader().getSender().iterator().next().getIdentifier().getValue()).isEqualTo(beforeConversion.getStandardBusinessDocumentHeader().getSender().iterator().next().getIdentifier().getValue());
    }
}
