package no.difi.meldingsutveksling.noarkexchange;

import net.logstash.logback.marker.LogstashMarker;
import net.logstash.logback.marker.Markers;
import no.difi.meldingsutveksling.IntegrasjonspunktNokkel;
import no.difi.meldingsutveksling.StandardBusinessDocumentConverter;
import no.difi.meldingsutveksling.dokumentpakking.domain.Archive;
import no.difi.meldingsutveksling.dokumentpakking.service.CmsUtil;
import no.difi.meldingsutveksling.dokumentpakking.service.CreateAsice;
import no.difi.meldingsutveksling.dokumentpakking.service.CreateSBD;
import no.difi.meldingsutveksling.dokumentpakking.xml.Payload;
import no.difi.meldingsutveksling.domain.Avsender;
import no.difi.meldingsutveksling.domain.BestEduMessage;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.Mottaker;
import no.difi.meldingsutveksling.domain.sbdh.Document;
import no.difi.meldingsutveksling.kvittering.xsd.Kvittering;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.noarkexchange.schema.ObjectFactory;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.receive.StandardBusinessDocument;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

import static no.difi.meldingsutveksling.logging.MessageMarkerFactory.markerFrom;
import static no.difi.meldingsutveksling.logging.MessageMarkerFactory.payloadSizeMarker;

/**
 * Factory class for StandardBusinessDocument instances
 */
@Component
public class StandardBusinessDocumentFactory {

    public static final String DOCUMENT_TYPE_MELDING = "melding";
    private static JAXBContext jaxbContextdomain;
    private static JAXBContext jaxbContext;

    @Autowired
    private IntegrasjonspunktNokkel integrasjonspunktNokkel;

    static {
        try {
            jaxbContext = JAXBContext.newInstance(StandardBusinessDocument.class, Payload.class, Kvittering.class);
            jaxbContextdomain = JAXBContext.newInstance(Document.class, Payload.class, Kvittering.class);
        } catch (JAXBException e) {
            throw new MeldingsUtvekslingRuntimeException("Could not initialize " + StandardBusinessDocumentConverter.class, e);
        }
    }

    public StandardBusinessDocumentFactory() {
    }

    public StandardBusinessDocumentFactory(IntegrasjonspunktNokkel integrasjonspunktNokkel) {
        this.integrasjonspunktNokkel = integrasjonspunktNokkel;
    }

    public Document create(PutMessageRequestType sender, Avsender avsender, Mottaker mottaker) throws MessageException {
        return create(sender, UUID.randomUUID().toString(), avsender, mottaker);
    }

    public Document create(PutMessageRequestType shipment, String conversationId, Avsender avsender, Mottaker mottaker) throws MessageException {
        final byte[] marshalledShipment = marshall(shipment);

        BestEduMessage bestEduMessage = new BestEduMessage(marshalledShipment);
        LogstashMarker marker = markerFrom(new PutMessageRequestWrapper(shipment));
        Audit.info("Payload size of message", marker.and(payloadSizeMarker(marshalledShipment)));
        Archive archive;
        try {
            archive = createAsicePackage(avsender, mottaker, bestEduMessage);
        } catch (IOException e) {
            throw new MessageException(e, StatusMessage.UNABLE_TO_CREATE_STANDARD_BUSINESS_DOCUMENT);
        }
        Payload payload = new Payload(encryptArchive(mottaker, archive));

        final JournalpostId journalpostId = JournalpostId.fromPutMessage(new PutMessageRequestWrapper(shipment));
        return new CreateSBD().createSBD(avsender.getOrgNummer(), mottaker.getOrgNummer(), payload, conversationId, DOCUMENT_TYPE_MELDING, journalpostId.value());
    }

    private byte[] encryptArchive(Mottaker mottaker, Archive archive) {
        return new CmsUtil().createCMS(archive.getBytes()
                , mottaker.getSertifikat());
    }

    private Archive createAsicePackage(Avsender avsender, Mottaker mottaker, BestEduMessage bestEduMessage) throws IOException {
        return new CreateAsice().createAsice(bestEduMessage, integrasjonspunktNokkel.getSignatureHelper(), avsender, mottaker);
    }

    private byte[] marshall(PutMessageRequestType message) {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(PutMessageRequestType.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            jaxbMarshaller.marshal(new ObjectFactory().createPutMessageRequest(message), os);
        } catch (JAXBException e) {
            throw new MeldingsUtvekslingRuntimeException(e);
        }
        return os.toByteArray();
    }

    public static Document create(StandardBusinessDocument fromDocument) {
        ModelMapper mapper = new ModelMapper();
        return mapper.map(fromDocument, Document.class);
    }

    public static StandardBusinessDocument create(Document fromDocument) {
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
}