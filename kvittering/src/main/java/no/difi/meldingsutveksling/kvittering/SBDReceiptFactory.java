package no.difi.meldingsutveksling.kvittering;


import no.difi.meldingsutveksling.IntegrasjonspunktNokkel;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.MessageInfo;
import no.difi.meldingsutveksling.domain.Organisasjonsnummer;
import no.difi.meldingsutveksling.domain.XMLTimeStamp;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocumentHeader;
import no.difi.meldingsutveksling.kvittering.xsd.Aapning;
import no.difi.meldingsutveksling.kvittering.xsd.Kvittering;
import no.difi.meldingsutveksling.kvittering.xsd.Levering;
import no.difi.meldingsutveksling.kvittering.xsd.ObjectFactory;
import org.apache.xml.security.exceptions.XMLSecurityException;

import javax.xml.xpath.XPathExpressionException;

import static no.difi.meldingsutveksling.kvittering.DocumentToDocumentConverter.toDomainDocument;
import static no.difi.meldingsutveksling.kvittering.DocumentToDocumentConverter.toXMLDocument;

/**
 * Factory class for creating Kvittering documents. This class is the only one visible from outside and uses
 * the other package local classes to perform its tasks.
 * //todo move StandardBusinessDocumentFactory to domain, and remove the two "borrowed" private create methods
 *
 * @author Glenn bech
 */
public class SBDReceiptFactory {

    private SBDReceiptFactory() {
    }

    public static StandardBusinessDocument createAapningskvittering(MessageInfo messageInfo, IntegrasjonspunktNokkel keyInfo) {
        Kvittering k = new Kvittering();
        k.setAapning(new Aapning());
        k.setTidspunkt(XMLTimeStamp.createTimeStamp());
        return signAndWrapDocument(messageInfo, keyInfo, k);
    }

    public static StandardBusinessDocument createLeveringsKvittering(MessageInfo messageInfo, IntegrasjonspunktNokkel keyInfo) {
        Kvittering k = new Kvittering();
        k.setLevering(new Levering());
        k.setTidspunkt(XMLTimeStamp.createTimeStamp());
        return signAndWrapDocument(messageInfo,
                keyInfo,
                k);
    }

    public static StandardBusinessDocument createDpeReceiptFrom(StandardBusinessDocument sbd) {
        StandardBusinessDocument receipt = new StandardBusinessDocument();
        StandardBusinessDocumentHeader sbdh = new StandardBusinessDocumentHeader.Builder()
                .from(Organisasjonsnummer.from(sbd.getReceiverIdentifier()))
                .to(Organisasjonsnummer.from(sbd.getSenderIdentifier()))
                .relatedToConversationId(sbd.getConversationId())
                .type(StandardBusinessDocumentHeader.DocumentType.DPE_RECEIPT)
                .build();
        receipt.setStandardBusinessDocumentHeader(sbdh);
        receipt.setAny(sbd.getAny());
        return receipt;
    }

    private static StandardBusinessDocument signAndWrapDocument(MessageInfo messageInfo, IntegrasjonspunktNokkel keyInfo, Kvittering kvittering) {

        StandardBusinessDocument unsignedReceipt = new StandardBusinessDocument();
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
        org.w3c.dom.Document signedXmlDoc = DocumentSigner.sign(xmlDoc, keyInfo);
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
