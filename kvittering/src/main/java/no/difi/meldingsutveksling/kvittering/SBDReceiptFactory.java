package no.difi.meldingsutveksling.kvittering;


import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.DocumentType;
import no.difi.meldingsutveksling.IntegrasjonspunktNokkel;
import no.difi.meldingsutveksling.Process;
import no.difi.meldingsutveksling.Standard;
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
import no.difi.meldingsutveksling.nextmove.StatusMessage;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.springframework.stereotype.Component;

import javax.xml.xpath.XPathExpressionException;

import static no.difi.meldingsutveksling.kvittering.DocumentToDocumentConverter.toDomainDocument;
import static no.difi.meldingsutveksling.kvittering.DocumentToDocumentConverter.toXMLDocument;

/**
 * Factory class for creating Kvittering documents. This class is the only one visible from outside and uses
 * the other package local classes to perform its tasks.
 *
 * @author Glenn bech
 */
@Component
@RequiredArgsConstructor
public class SBDReceiptFactory {

    private final ServiceRegistryLookup serviceRegistryLookup;

    public StandardBusinessDocument createAapningskvittering(MessageInfo messageInfo, IntegrasjonspunktNokkel keyInfo) {
        Kvittering k = new Kvittering();
        k.setAapning(new Aapning());
        k.setTidspunkt(XMLTimeStamp.createTimeStamp());
        return signAndWrapDocument(messageInfo, keyInfo, k);
    }

    public StandardBusinessDocument createLeveringsKvittering(MessageInfo messageInfo, IntegrasjonspunktNokkel keyInfo) {
        Kvittering k = new Kvittering();
        k.setLevering(new Levering());
        k.setTidspunkt(XMLTimeStamp.createTimeStamp());
        return signAndWrapDocument(messageInfo,
                keyInfo,
                k);
    }

    public StandardBusinessDocument createArkivmeldingStatusFrom(StandardBusinessDocument sbd,
                                                                 DocumentType documentType,
                                                                 ReceiptStatus status) {
        String standard = serviceRegistryLookup.getStandard(sbd.getSenderIdentifier(), Process.ARKIVMELDING_RESPONSE, documentType);
        StatusMessage statusMessage = new StatusMessage(status);

        return new StandardBusinessDocument()
                .setStandardBusinessDocumentHeader(new StandardBusinessDocumentHeader.Builder()
                        .from(Organisasjonsnummer.from(sbd.getReceiverIdentifier()))
                        .to(Organisasjonsnummer.from(sbd.getSenderIdentifier()))
                        .relatedToConversationId(sbd.getConversationId())
                        .process(Process.ARKIVMELDING_RESPONSE)
                        .standard(standard)
                        .type(documentType)
                        .build())
                .setAny(statusMessage);
    }

    public StandardBusinessDocument createEinnsynStatusFrom(StandardBusinessDocument sbd,
                                                            DocumentType documentType,
                                                            ReceiptStatus status) {
        String standard = serviceRegistryLookup.getStandard(sbd.getSenderIdentifier(), Process.EINNSYN_RESPONSE, documentType);
        StatusMessage statusMessage = new StatusMessage(status);

        return new StandardBusinessDocument()
                .setStandardBusinessDocumentHeader(new StandardBusinessDocumentHeader.Builder()
                        .from(Organisasjonsnummer.from(sbd.getReceiverIdentifier()))
                        .to(Organisasjonsnummer.from(sbd.getSenderIdentifier()))
                        .relatedToConversationId(sbd.getConversationId())
                        .process(Process.EINNSYN_RESPONSE)
                        .standard(standard)
                        .type(documentType)
                        .build())
                .setAny(statusMessage);
    }

    private StandardBusinessDocument signAndWrapDocument(MessageInfo messageInfo, IntegrasjonspunktNokkel keyInfo, Kvittering kvittering) {

        StandardBusinessDocument unsignedReceipt = new StandardBusinessDocument();
        StandardBusinessDocumentHeader header = new StandardBusinessDocumentHeader.Builder()
                // sender of the receipt is the receiver of the message
                .from(new Organisasjonsnummer(messageInfo.getReceiverOrgNumber()))
                .to(new Organisasjonsnummer(messageInfo.getSenderOrgNumber()))
                .relatedToConversationId(messageInfo.getConversationId())
                .relatedToJournalPostId(messageInfo.getJournalPostId())
                .process(Process.LEGACY)
                .standard(Standard.LEGACY.getValue())
                .type(DocumentType.BESTEDU_MELDING)
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
