package no.difi.meldingsutveksling.kvittering;


import no.difi.meldingsutveksling.StandardBusinessDocumentConverter;
import no.difi.meldingsutveksling.dokumentpakking.xml.Payload;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.Organisasjonsnummer;
import no.difi.meldingsutveksling.domain.XMLTimeStamp;
import no.difi.meldingsutveksling.domain.sbdh.Document;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocumentHeader;
import no.difi.meldingsutveksling.kvittering.xsd.Aapning;
import no.difi.meldingsutveksling.kvittering.xsd.Kvittering;
import no.difi.meldingsutveksling.kvittering.xsd.Levering;
import no.difi.meldingsutveksling.kvittering.xsd.ObjectFactory;
import no.difi.meldingsutveksling.noarkexchange.schema.receive.StandardBusinessDocument;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
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
public class KvitteringFactory {

    private static final JAXBContext jaxbContextdomain;
    private static final JAXBContext jaxbContext;

    static {
        try {
            jaxbContext = JAXBContext.newInstance(StandardBusinessDocument.class, Payload.class, Kvittering.class);
            jaxbContextdomain = JAXBContext.newInstance(Document.class, Payload.class, Kvittering.class);
        } catch (JAXBException e) {
            throw new MeldingsUtvekslingRuntimeException("Could not initialize " + StandardBusinessDocumentConverter.class, e);
        }
    }

    /**
     * Creates an Åpningskvittering
     *
     * @param receiverOrgNumber
     * @param senderOrgNumber
     * @param journalPostId
     * @param conversationId
     * @param keyPair
     */
    public static Document createAapningskvittering(String receiverOrgNumber, String senderOrgNumber,
                                                    String journalPostId, String conversationId, KeyPair keyPair) {
        Kvittering k = new Kvittering();
        k.setAapning(new Aapning());
        k.setTidspunkt(XMLTimeStamp.createTimeStamp());
        return signAndWrapDocument(receiverOrgNumber, senderOrgNumber, journalPostId, conversationId, keyPair, k);
    }

    public static Document createLeveringsKvittering(String receiverOrgNumber, String senderOrgNumber,
                                                     String journalPostId, String conversationId, KeyPair keyPair) {
        Kvittering k = new Kvittering();
        k.setLevering(new Levering());
        k.setTidspunkt(XMLTimeStamp.createTimeStamp());
        return signAndWrapDocument(receiverOrgNumber, senderOrgNumber, journalPostId, conversationId, keyPair, k);
    }

    private static Document signAndWrapDocument(String receiverOrgNumber, String senderOrgNumber, String journalPostId,
                                                String conversationId, KeyPair keyPair, Kvittering kvittering) {

        Document unsignedReceipt = new Document();
        StandardBusinessDocumentHeader header = new StandardBusinessDocumentHeader.Builder()
                .from(new Organisasjonsnummer(senderOrgNumber))
                .to(new Organisasjonsnummer(receiverOrgNumber))
                .relatedToConversationId(conversationId)
                .relatedToJournalPostId(journalPostId)
                .build();

        unsignedReceipt.setStandardBusinessDocumentHeader(header);
        unsignedReceipt.setAny(new ObjectFactory().createKvittering(kvittering));

        org.w3c.dom.Document xmlDoc = toXMLDocument(unsignedReceipt);
        org.w3c.dom.Document signedXmlDoc = DocumentSigner.sign(xmlDoc, keyPair);
        if (!DocumentValidator.validate(signedXmlDoc)) {
            throw new MeldingsUtvekslingRuntimeException("created non validating document");
        }
        return toDomainDocument(signedXmlDoc);
    }

}
