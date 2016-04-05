package no.difi.meldingsutveksling.kvittering;


import no.difi.meldingsutveksling.StandardBusinessDocumentConverter;
import no.difi.meldingsutveksling.dokumentpakking.xml.Payload;
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
            jaxbContextdomain = JAXBContext.newInstance(EduDocument.class, Payload.class, Kvittering.class);
        } catch (JAXBException e) {
            throw new MeldingsUtvekslingRuntimeException("Could not initialize " + StandardBusinessDocumentConverter.class, e);
        }
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
                .from(new Organisasjonsnummer(messageInfo.getSenderOrgNumber()))
                .to(new Organisasjonsnummer(messageInfo.getReceiverOrgNumber()))
                .relatedToConversationId(messageInfo.getConversationId())
                .relatedToJournalPostId(messageInfo.getJournalPostId())
                .type(StandardBusinessDocumentHeader.DocumentType.KVITTERING)
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
