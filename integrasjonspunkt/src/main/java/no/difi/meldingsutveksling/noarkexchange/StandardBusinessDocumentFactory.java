package no.difi.meldingsutveksling.noarkexchange;

import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.marker.LogstashMarker;
import no.difi.meldingsutveksling.IntegrasjonspunktNokkel;
import no.difi.meldingsutveksling.MimeTypeExtensionMapper;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.core.EDUCore;
import no.difi.meldingsutveksling.core.EDUCoreConverter;
import no.difi.meldingsutveksling.dokumentpakking.domain.Archive;
import no.difi.meldingsutveksling.dokumentpakking.service.CmsUtil;
import no.difi.meldingsutveksling.dokumentpakking.service.CreateAsice;
import no.difi.meldingsutveksling.dokumentpakking.service.CreateSBD;
import no.difi.meldingsutveksling.domain.*;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocumentHeader;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.nextmove.ConversationResource;
import no.difi.meldingsutveksling.nextmove.message.FileEntryStream;
import no.difi.meldingsutveksling.nextmove.message.MessagePersister;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static no.difi.meldingsutveksling.ServiceIdentifier.DPE_INNSYN;
import static no.difi.meldingsutveksling.ServiceIdentifier.DPE_RECEIPT;
import static no.difi.meldingsutveksling.core.EDUCoreMarker.markerFrom;
import static no.difi.meldingsutveksling.logging.MessageMarkerFactory.payloadSizeMarker;

/**
 * Factory class for StandardBusinessDocument instances
 */
@Component
@Slf4j
public class StandardBusinessDocumentFactory {

    public static final String DOCUMENT_TYPE_MELDING = "melding";
    @Autowired
    private IntegrasjonspunktNokkel integrasjonspunktNokkel;

    @Autowired
    private IntegrasjonspunktProperties props;

    private MessagePersister messagePersister;

    public StandardBusinessDocumentFactory(IntegrasjonspunktNokkel integrasjonspunktNokkel, ObjectProvider<MessagePersister> messagePersister) {
        this.integrasjonspunktNokkel = integrasjonspunktNokkel;
        this.messagePersister = messagePersister.getIfUnique();
    }

    public StandardBusinessDocument create(EDUCore sender, Avsender avsender, Mottaker mottaker) throws MessageException {
        return create(sender, UUID.randomUUID().toString(), avsender, mottaker);
    }

    public StandardBusinessDocument create(EDUCore shipment, String conversationId, Avsender avsender, Mottaker mottaker) throws MessageException {
        byte[] marshalledShipment = EDUCoreConverter.marshallToBytes(shipment);

        BestEduMessage bestEduMessage = new BestEduMessage(marshalledShipment);
        LogstashMarker marker = markerFrom(shipment);
        Audit.info("Payload size", marker.and(payloadSizeMarker(marshalledShipment)));
        Archive archive;
        try {
            archive = createAsicePackage(avsender, mottaker, bestEduMessage);
        } catch (IOException e) {
            throw new MessageException(e, StatusMessage.UNABLE_TO_CREATE_STANDARD_BUSINESS_DOCUMENT);
        }
        Payload payload = new Payload(encryptArchive(mottaker, archive, shipment.getServiceIdentifier()));

        return new CreateSBD().createSBD(avsender.getOrgNummer(), mottaker.getOrgNummer(), payload, conversationId, DOCUMENT_TYPE_MELDING, shipment.getJournalpostId());
    }

    public StandardBusinessDocument create(ConversationResource cr, MessageContext context) throws MessageException {
        List<StreamedFile> attachements = new ArrayList<>();
        if (cr.getFileRefs() != null) {
            for (String filename : cr.getFileRefs().values()) {
                FileEntryStream fileEntryStream = messagePersister.readStream(cr, filename);
                String ext = Stream.of(filename.split(".")).reduce((p, e) -> e).orElse("pdf");
                attachements.add(new NextMoveStreamedFile(filename, fileEntryStream.getInputStream(), MimeTypeExtensionMapper.getMimetype(ext)));
            }
        }

        PipedOutputStream archiveOutputStream = new PipedOutputStream();
        CompletableFuture.runAsync(() -> {
            log.debug("Starting thread: create asic");
            try {
                createAsicePackage(context.getAvsender(), context.getMottaker(), attachements, archiveOutputStream);
                for (StreamedFile a : attachements) {
                    a.getInputStream().close();
                }
                archiveOutputStream.close();
            } catch (IOException e) {
                throw new MeldingsUtvekslingRuntimeException(StatusMessage.UNABLE_TO_CREATE_STANDARD_BUSINESS_DOCUMENT.getTechnicalMessage(), e);
            }
            log.debug("Thread finished: create asic");
        });

        PipedInputStream archiveInputStream;
        try {
            archiveInputStream = new PipedInputStream(archiveOutputStream);
        } catch (IOException e) {
            String errorMsg = "Error creating PipedInputStream from ASiC";
            log.error(errorMsg);
            throw new MeldingsUtvekslingRuntimeException(errorMsg, e);
        }
        PipedOutputStream encryptedOutputStream = new PipedOutputStream();
        CompletableFuture.runAsync(() -> {
            log.debug("Starting thread: encrypt archive");
            encryptArchive(context.getMottaker(), cr.getServiceIdentifier(), archiveInputStream, encryptedOutputStream);
            try {
                encryptedOutputStream.close();
            } catch (IOException e) {
                log.error("Error closing encryption stream");
            }
            log.debug("Thread finished: encrypt archive");
        });

        PipedInputStream encryptedInputStream;
        try {
            encryptedInputStream = new PipedInputStream(encryptedOutputStream);
        } catch (IOException e) {
            String errorMsg = "Error creating PipedInputStream from encrypted ASiC";
            log.error(errorMsg);
            throw new MeldingsUtvekslingRuntimeException(errorMsg, e);
        }
        Payload payload = new Payload(encryptedInputStream, cr);

        return new CreateSBD().createSBD(context.getAvsender().getOrgNummer(), context.getMottaker().getOrgNummer(),
                payload, context.getConversationId(), StandardBusinessDocumentHeader.NEXTMOVE_TYPE,
                context.getJournalPostId());
    }

    private void encryptArchive(Mottaker mottaker, ServiceIdentifier serviceIdentifier, InputStream archive, OutputStream encrypted) {
        Set<ServiceIdentifier> standardEncryptionUsers = EnumSet.of(DPE_INNSYN, DPE_RECEIPT);

        CmsUtil cmsUtil;
        if(standardEncryptionUsers.contains(serviceIdentifier)){

            cmsUtil = new CmsUtil(null);
        }else{

            cmsUtil = new CmsUtil();
        }

        cmsUtil.createCMSStreamed(archive, encrypted, (X509Certificate) mottaker.getSertifikat());
    }

    private byte[] encryptArchive(Mottaker mottaker, Archive archive, ServiceIdentifier serviceIdentifier) {

        Set<ServiceIdentifier> standardEncryptionUsers = EnumSet.of(DPE_INNSYN, DPE_RECEIPT);

        CmsUtil cmsUtil;
        if(standardEncryptionUsers.contains(serviceIdentifier)){

            cmsUtil = new CmsUtil(null);
        }else{

            cmsUtil = new CmsUtil();
        }

        return cmsUtil.createCMS(archive.getBytes(), (X509Certificate) mottaker.getSertifikat());
    }

    private Archive createAsicePackage(Avsender avsender, Mottaker mottaker, ByteArrayFile byteArrayFile) throws
            IOException {
        return new CreateAsice().createAsice(byteArrayFile, integrasjonspunktNokkel.getSignatureHelper(), avsender, mottaker);
    }

    private void createAsicePackage(Avsender avsender, Mottaker mottaker, List<StreamedFile> streamedFiles, OutputStream archive) throws
            IOException {
        new CreateAsice().createAsiceStreamed(streamedFiles, archive, integrasjonspunktNokkel.getSignatureHelper(), avsender, mottaker);
    }

}