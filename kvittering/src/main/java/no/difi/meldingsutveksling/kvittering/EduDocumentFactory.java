package no.difi.meldingsutveksling.kvittering;


import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.MessageInfo;
import no.difi.meldingsutveksling.domain.Organisasjonsnummer;
import no.difi.meldingsutveksling.domain.XMLTimeStamp;
import no.difi.meldingsutveksling.domain.sbdh.EduDocument;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocumentHeader;
import no.difi.meldingsutveksling.kvittering.xsd.Aapning;
import no.difi.meldingsutveksling.kvittering.xsd.Kvittering;
import no.difi.meldingsutveksling.kvittering.xsd.Levering;
import no.difi.meldingsutveksling.kvittering.xsd.ObjectFactory;
import org.apache.xml.security.exceptions.XMLSecurityException;

import javax.xml.xpath.XPathExpressionException;
import java.security.KeyPair;

import static no.difi.meldingsutveksling.kvittering.DocumentToDocumentConverter.toDomainDocument;
import static no.difi.meldingsutveksling.kvittering.DocumentToDocumentConverter.toXMLDocument;

/**
 * Factory class for creating Kvittering documents. This class is the only one visible from outside and uses
 * the other package local classes to perform its tasks.
 * //todo move StandardBusinessDocumentFactory to domain, and remove the two "borrowed" private create methods
 *
 * @author Glenn bech
 */
public class EduDocumentFactory {

    private EduDocumentFactory() {
    }

    public static EduDocument createAapningskvittering(MessageInfo messageInfo, KeyPair keyPair) {
        Kvittering k = new Kvittering();
        k.setAapning(new Aapning());
        k.setTidspunkt(XMLTimeStamp.createTimeStamp());
        return signAndWrapDocument(messageInfo, keyPair, k);
    }

    public static EduDocument createLeveringsKvittering(MessageInfo messageInfo, KeyPair keyPair) {
        Kvittering k = new Kvittering();
        k.setLevering(new Levering());
        k.setTidspunkt(XMLTimeStamp.createTimeStamp());
        return signAndWrapDocument(messageInfo,
                keyPair,
                k);
    }

    private static EduDocument signAndWrapDocument(MessageInfo messageInfo, KeyPair keyPair, Kvittering kvittering) {

        EduDocument unsignedReceipt = new EduDocument();
        StandardBusinessDocumentHeader header = new StandardBusinessDocumentHeader.Builder()
                // sender of the receipt is the receiver of the message
                .from(new Organisasjonsnummer(messageInfo.getReceiverOrgNumber()))
                .to(new Organisasjonsnummer(messageInfo.getSenderOrgNumber()))
                .relatedToConversationId(messageInfo.getConversationId())
                .relatedToJournalPostId(messageInfo.getJournalPostId())
                .type(StandardBusinessDocumentHeader.DocumentType.KVITTERING)
                .build();

        unsignedReceipt.setStandardBusinessDocumentHeader(header);
        unsignedReceipt.setAny(new ObjectFactory().createKvittering(kvittering));

        org.w3c.dom.Document xmlDoc = toXMLDocument(unsignedReceipt);
        org.w3c.dom.Document signedXmlDoc = DocumentSigner.sign(xmlDoc, keyPair);
        try {
            if (!DocumentValidator.validate(signedXmlDoc)) {
                throw new MeldingsUtvekslingRuntimeException("created non validating document");
            }
        } catch (XPathExpressionException | XMLSecurityException e) {
            throw new RuntimeException("Could not validate signature that we used to sign the document", e);
        }
        return toDomainDocument(signedXmlDoc);
    }
}
