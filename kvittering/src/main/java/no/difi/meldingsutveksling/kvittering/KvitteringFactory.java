package no.difi.meldingsutveksling.kvittering;


import no.difi.meldingsutveksling.StandardBusinessDocumentConverter;
import no.difi.meldingsutveksling.dokumentpakking.xml.Payload;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.Organisasjonsnummer;
import no.difi.meldingsutveksling.domain.sbdh.Document;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocumentHeader;
import no.difi.meldingsutveksling.kvittering.xsd.Aapning;
import no.difi.meldingsutveksling.kvittering.xsd.Kvittering;
import no.difi.meldingsutveksling.kvittering.xsd.Levering;
import no.difi.meldingsutveksling.kvittering.xsd.ObjectFactory;
import no.difi.meldingsutveksling.noarkexchange.schema.receive.StandardBusinessDocument;
import org.modelmapper.ModelMapper;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.KeyPair;
import java.util.GregorianCalendar;

import static no.difi.meldingsutveksling.kvittering.DocumentToDocumentConverter.*;

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
            throw new RuntimeException("Could not initialize " + StandardBusinessDocumentConverter.class, e);
        }
    }

    /**
     * Creates an Åpningskvittering
     *
     * @param receiverOrgNumber
     * @param senderOrgNumber
     * @param journalPostId
     * @param conversationId
     * @param keyPair           @return
     */
    public static Document createAapningskvittering(String receiverOrgNumber, String senderOrgNumber,
                                                    String journalPostId, String conversationId, KeyPair keyPair) {
        Kvittering k = new Kvittering();
        k.setAapning(new Aapning());
        k.setTidspunkt(createTimeStamp());
        return signAndWrapDocument(receiverOrgNumber, senderOrgNumber, journalPostId, conversationId, keyPair, k);
    }

    public static Document createLeveringsKvittering(String receiverOrgNumber, String senderOrgNumber,
                                                     String journalPostId, String conversationId, KeyPair keyPair) {
        Kvittering k = new Kvittering();
        k.setLevering(new Levering());
        k.setTidspunkt(createTimeStamp());
        return signAndWrapDocument(receiverOrgNumber, senderOrgNumber, journalPostId, conversationId, keyPair, k);
    }

    private static Document signAndWrapDocument(String receiverOrgNumber, String senderOrgNumber, String journalPostId,
                                                String conversationId, KeyPair keyPair, Kvittering kvittering) {

        Document unsignedReceipt = new Document();
        StandardBusinessDocumentHeader header = new StandardBusinessDocumentHeader.Builder()
                .from(new Organisasjonsnummer(receiverOrgNumber))
                .to(new Organisasjonsnummer(senderOrgNumber))
                .relatedToConversationId(conversationId)
                .relatedToJournalPostId(journalPostId)
                .build();

        unsignedReceipt.setStandardBusinessDocumentHeader(header);
        unsignedReceipt.setAny(new ObjectFactory().createKvittering(kvittering));

        org.w3c.dom.Document xmlDoc = toXMLDocument(unsignedReceipt);
        org.w3c.dom.Document signedXmlDoc = DocumentSigner.sign(xmlDoc, keyPair);
        return toDomainDocument(signedXmlDoc);
    }

    private static XMLGregorianCalendar createTimeStamp() {
        try {
            GregorianCalendar gcal = (GregorianCalendar) GregorianCalendar.getInstance();
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(gcal);
        } catch (DatatypeConfigurationException e) {
            throw new MeldingsUtvekslingRuntimeException(e);
        }
    }

    /**
     * @param fromDocument
     * @return
     */
    private static StandardBusinessDocument create(Document fromDocument) {
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            JAXBElement<Document> d = new no.difi.meldingsutveksling.domain.sbdh.ObjectFactory().createStandardBusinessDocument(fromDocument);

            jaxbContextdomain.createMarshaller().marshal(d, os);
            byte[] tmp = os.toByteArray();

            JAXBElement<no.difi.meldingsutveksling.noarkexchange.schema.receive.StandardBusinessDocument> toDocument
                    = (JAXBElement<no.difi.meldingsutveksling.noarkexchange.schema.receive.StandardBusinessDocument>)
                    jaxbContext.createUnmarshaller().unmarshal(new ByteArrayInputStream(tmp));

            return toDocument.getValue();
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    public static Document create(StandardBusinessDocument fromDocument) {
        ModelMapper mapper = new ModelMapper();
        return mapper.map(fromDocument, Document.class);
    }
}
