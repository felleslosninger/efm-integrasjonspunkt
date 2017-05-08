package no.difi.meldingsutveksling.noarkexchange;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import net.logstash.logback.marker.LogstashMarker;
import no.difi.meldingsutveksling.IntegrasjonspunktNokkel;
import no.difi.meldingsutveksling.StandardBusinessDocumentConverter;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.core.EDUCore;
import no.difi.meldingsutveksling.dokumentpakking.domain.Archive;
import no.difi.meldingsutveksling.dokumentpakking.service.CmsUtil;
import no.difi.meldingsutveksling.dokumentpakking.service.CreateAsice;
import no.difi.meldingsutveksling.dokumentpakking.service.CreateSBD;
import no.difi.meldingsutveksling.dokumentpakking.xml.Payload;
import no.difi.meldingsutveksling.domain.*;
import no.difi.meldingsutveksling.domain.sbdh.EduDocument;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocumentHeader;
import no.difi.meldingsutveksling.kvittering.xsd.Kvittering;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.nextbest.ConversationResource;
import no.difi.meldingsutveksling.noarkexchange.receive.EDUCoreConverter;
import no.difi.meldingsutveksling.noarkexchange.schema.receive.StandardBusinessDocument;
import org.apache.commons.io.FileUtils;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static no.difi.meldingsutveksling.core.EDUCoreMarker.markerFrom;
import static no.difi.meldingsutveksling.logging.MessageMarkerFactory.payloadSizeMarker;

/**
 * Factory class for StandardBusinessDocument instances
 */
@Component
public class StandardBusinessDocumentFactory {

    Logger log = LoggerFactory.getLogger(StandardBusinessDocumentFactory.class);

    public static final String DOCUMENT_TYPE_MELDING = "melding";
    private static JAXBContext jaxbContextdomain;
    private static JAXBContext jaxbContext;

    @Autowired
    private IntegrasjonspunktNokkel integrasjonspunktNokkel;

    @Autowired
    private IntegrasjonspunktProperties props;

    static {
        try {
            jaxbContext = JAXBContext.newInstance(StandardBusinessDocument.class, Payload.class, Kvittering.class);
            jaxbContextdomain = JAXBContext.newInstance(EduDocument.class, Payload.class, Kvittering.class);
        } catch (JAXBException e) {
            throw new MeldingsUtvekslingRuntimeException("Could not initialize " + StandardBusinessDocumentConverter.class, e);
        }
    }

    public StandardBusinessDocumentFactory() {
    }

    public StandardBusinessDocumentFactory(IntegrasjonspunktNokkel integrasjonspunktNokkel) {
        this.integrasjonspunktNokkel = integrasjonspunktNokkel;
    }

    public EduDocument create(EDUCore sender, Avsender avsender, Mottaker mottaker) throws MessageException {
        return create(sender, UUID.randomUUID().toString(), avsender, mottaker);
    }

    public EduDocument create(EDUCore shipment, String conversationId, Avsender avsender, Mottaker mottaker) throws MessageException {
        EDUCoreConverter eduCoreConverter = new EDUCoreConverter();
        byte[] marshalledShipment = eduCoreConverter.marshallToBytes(shipment);
        if (shipment.getMessageType() == EDUCore.MessageType.APPRECEIPT) {
            // MOVE-191: Need to remove ns prefix due to it giving errors in ephorte
            marshalledShipment = new String(marshalledShipment).replaceAll(":ns2|ns2:", "").getBytes();
        }

        BestEduMessage bestEduMessage = new BestEduMessage(marshalledShipment);
        LogstashMarker marker = markerFrom(shipment);
        Audit.info("Payload size", marker.and(payloadSizeMarker(marshalledShipment)));
        Archive archive;
        try {
            archive = createAsicePackage(avsender, mottaker, bestEduMessage);
        } catch (IOException e) {
            throw new MessageException(e, StatusMessage.UNABLE_TO_CREATE_STANDARD_BUSINESS_DOCUMENT);
        }
        Payload payload = new Payload(encryptArchive(mottaker, archive));

        return new CreateSBD().createSBD(avsender.getOrgNummer(), mottaker.getOrgNummer(), payload, conversationId, DOCUMENT_TYPE_MELDING, shipment.getJournalpostId());
    }

    public EduDocument create(ConversationResource shipmentMeta, MessageContext context) throws MessageException {

        List<ByteArrayFile> attachements = new ArrayList<>();
        ObjectMapper om = new ObjectMapper();
        om.registerModule(new JavaTimeModule());
        byte[] json;
        try {
            json = om.writeValueAsBytes(shipmentMeta);
        } catch (JsonProcessingException e) {
            throw new MessageException(e, StatusMessage.UNABLE_TO_MARSHALL_CONVERSATION);
        }
        attachements.add(new NextBestAttachement(json, "conversation.json"));

        for (String filename : shipmentMeta.getFileRefs().values()) {
            String filedir = props.getNextbest().getFiledir();
            if (!filedir.endsWith("/")) {
                filedir = filedir+"/";
            }
            filedir = filedir+shipmentMeta.getConversationId()+"/";
            File file = new File(filedir+filename);

            byte[] bytes;
            try {
                bytes = FileUtils.readFileToByteArray(file);
            } catch (IOException e) {
                log.error("Could not read file \""+file.getName()+"\"", e);
                throw new MessageException(e, StatusMessage.UNABLE_TO_CREATE_STANDARD_BUSINESS_DOCUMENT);
            }
            attachements.add(new NextBestAttachement(bytes, filename));
        }

        Archive archive;
        try {
            archive = createAsicePackage(context.getAvsender(), context.getMottaker(), attachements);
        } catch (IOException e) {
            throw new MessageException(e, StatusMessage.UNABLE_TO_CREATE_STANDARD_BUSINESS_DOCUMENT);
        }
        Payload payload = new Payload(encryptArchive(context.getMottaker(), archive));

        return new CreateSBD().createSBD(context.getAvsender().getOrgNummer(), context.getMottaker().getOrgNummer(),
                payload, context.getConversationId(), StandardBusinessDocumentHeader.NEXTBEST_TYPE,
                context.getJournalPostId(), shipmentMeta.getMessagetypeId());
    }

    private byte[] encryptArchive(Mottaker mottaker, Archive archive) {
        return new CmsUtil().createCMS(archive.getBytes()
                , (X509Certificate) mottaker.getSertifikat());
    }

    private Archive createAsicePackage(Avsender avsender, Mottaker mottaker, ByteArrayFile byteArrayFile) throws
            IOException {
        return new CreateAsice().createAsice(byteArrayFile, integrasjonspunktNokkel.getSignatureHelper(), avsender, mottaker);
    }

    private Archive createAsicePackage(Avsender avsender, Mottaker mottaker, List<ByteArrayFile> byteArrayFile) throws
            IOException {
        return new CreateAsice().createAsice(byteArrayFile, integrasjonspunktNokkel.getSignatureHelper(), avsender, mottaker);
    }

    public static EduDocument create(StandardBusinessDocument fromDocument) {
        ModelMapper mapper = new ModelMapper();
        return mapper.map(fromDocument, EduDocument.class);
    }

    public static StandardBusinessDocument create(EduDocument fromDocument) {
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            JAXBElement<EduDocument> d = new no.difi.meldingsutveksling.domain.sbdh.ObjectFactory().createStandardBusinessDocument(fromDocument);

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