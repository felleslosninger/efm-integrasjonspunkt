package no.difi.meldingsutveksling.nextmove.v2;

import com.querydsl.core.types.Predicate;
import lombok.RequiredArgsConstructor;
import no.arkivverket.standarder.noark5.arkivmelding.Arkivmelding;
import no.difi.meldingsutveksling.*;
import no.difi.meldingsutveksling.arkivmelding.ArkivmeldingException;
import no.difi.meldingsutveksling.arkivmelding.ArkivmeldingUtil;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.exceptions.*;
import no.difi.meldingsutveksling.nextmove.*;
import no.difi.meldingsutveksling.nextmove.message.CryptoMessagePersister;
import no.difi.meldingsutveksling.nextmove.message.FileEntryStream;
import no.difi.meldingsutveksling.noarkexchange.receive.InternalQueue;
import no.difi.meldingsutveksling.receipt.ConversationService;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Strings.emptyToNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Arrays.asList;
import static no.difi.meldingsutveksling.ServiceIdentifier.*;

@Component
@RequiredArgsConstructor
public class NextMoveMessageService {

    private final CryptoMessagePersister cryptoMessagePersister;
    private final NextMoveMessageOutRepository messageRepo;
    private final InternalQueue internalQueue;
    private final ConversationService conversationService;
    private final ServiceRegistryLookup sr;
    private final ServiceIdentifierService serviceIdentifierService;
    private final UUIDGenerator uuidGenerator;

    NextMoveOutMessage getMessage(String conversationId) {
        return messageRepo.findByConversationId(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));
    }

    Page<NextMoveOutMessage> findMessages(Predicate predicate, Pageable pageable) {
        return messageRepo.findAll(predicate, pageable);
    }

    NextMoveOutMessage createMessage(StandardBusinessDocument sbd) {
        sbd.getOptionalConversationId()
                .flatMap(messageRepo::findByConversationId)
                .map(p -> {
                    throw new ConversationAlreadyExistsException(p.getConversationId());
                });

        ServiceRecord serviceRecord = getServiceRecord(sbd);
        ServiceIdentifier serviceIdentifier = getServiceIdentifier(serviceRecord);

        if (!serviceIdentifierService.isEnabled(serviceIdentifier)) {
            throw new ServiceNotEnabledException(serviceIdentifier);
        }

        DocumentType documentType = DocumentType.valueOf(sbd.getMessageType(), ApiType.NEXTMOVE)
                .orElseThrow(() -> new UnknownNextMoveDocumentTypeException(sbd.getMessageType()));

        String standard = sbd.getStandard();

        if (!documentType.fitsStandard(standard)) {
            throw new DocumentTypeDoNotFitDocumentStandardException(documentType, standard);
        }

        if (!serviceRecord.hasStandard(standard)) {
            throw new ReceiverDoNotAcceptDocumentStandard(standard, sbd.getProcess());
        }

        setDefaults(sbd, serviceIdentifier);
        NextMoveOutMessage message = NextMoveOutMessage.of(sbd, serviceIdentifier);
        messageRepo.save(message);
        conversationService.registerConversation(message);

        return message;
    }

    private ServiceIdentifier getServiceIdentifier(ServiceRecord serviceRecord) {
        ServiceIdentifier serviceIdentifier = serviceRecord.getServiceIdentifier();
        return serviceIdentifier == DPF ? DPO : serviceIdentifier;
    }

    private ServiceRecord getServiceRecord(StandardBusinessDocument sbd) {
        return sr.getServiceRecordByProcess(sbd.getReceiverIdentifier(), sbd.getProcess())
                .orElseThrow(() -> new ReceiverDoNotAcceptProcessException(sbd.getProcess()));
    }

    private void setDefaults(StandardBusinessDocument sbd, ServiceIdentifier serviceIdentifier) {
        sbd.getScopes()
                .stream()
                .filter(p -> isNullOrEmpty(p.getInstanceIdentifier()))
                .forEach(p -> p.setInstanceIdentifier(createConversationId()));

        if (serviceIdentifier == DPI) {
            setDpiDefaults(sbd);
        }
    }

    private void setDpiDefaults(StandardBusinessDocument sbd) {
        Optional<ServiceRecord> serviceRecord = sr.getServiceRecord(sbd.getReceiverIdentifier(), DPI);
        ServiceRecord record = serviceRecord.orElseThrow(() -> new NextMoveRuntimeException(String.format("No service record of type %s found for receiver", DPI.toString())));
        if (DocumentType.getOrThrow(sbd.getMessageType()) == DocumentType.PRINT) {
            DpiPrintMessage dpiMessage = (DpiPrintMessage) sbd.getAny();
            if (dpiMessage.getReceiver() == null) {
                dpiMessage.setReceiver(new PostAddress());
            }

            setReceiverDefaults(dpiMessage.getReceiver(), record.getPostAddress());
            setReceiverDefaults(dpiMessage.getMailReturn().getReceiver(), record.getReturnAddress());
        }
    }

    private void setReceiverDefaults(PostAddress receiver, no.difi.meldingsutveksling.serviceregistry.externalmodel.PostAddress srReceiver) {
        if (isNullOrEmpty(receiver.getName())) {
            receiver.setName(srReceiver.getName());
        }
        if (isNullOrEmpty(receiver.getAddressLine1())) {
            receiver.setAddressLine1(srReceiver.getStreet());
        }
        if (isNullOrEmpty(receiver.getPostalCode())) {
            receiver.setPostalCode(srReceiver.getPostalCode());
        }
        if (isNullOrEmpty(receiver.getPostalArea())) {
            receiver.setPostalArea(srReceiver.getPostalArea());
        }
        if (isNullOrEmpty(receiver.getCountry())) {
            receiver.setCountryCode(srReceiver.getCountry());
        }
    }

    void addFile(NextMoveOutMessage message, MultipartFile file) {
        try {
            addFile(message, file.getName(), file.getOriginalFilename(), file.getContentType(), file.getInputStream(), file.getSize());
        } catch (IOException e) {
            throw new InputStreamException(file.getOriginalFilename());
        }
    }

    void addFile(
            NextMoveOutMessage message,
            String title,
            String filename,
            String contentType,
            InputStream inputStream,
            long size) {

        Set<BusinessMessageFile> files = message.getOrCreateFiles();

        boolean primaryDocument = isPrimaryDocument(message, filename);

        if (primaryDocument && files.stream().anyMatch(BusinessMessageFile::getPrimaryDocument)) {
            throw new MultiplePrimaryDocumentsNotAllowedException();
        }

        List<ServiceIdentifier> requiredTitleCapabilities = asList(DPV, DPI);
        if (requiredTitleCapabilities.contains(message.getServiceIdentifier()) && isNullOrEmpty(title)) {
            throw new MissingFileTitleException(requiredTitleCapabilities.stream()
                    .map(ServiceIdentifier::toString)
                    .collect(Collectors.joining(",")));
        }

        BusinessMessageFile file = new BusinessMessageFile()
                .setIdentifier(UUID.randomUUID().toString())
                .setTitle(emptyToNull(title))
                .setFilename(filename)
                .setMimetype(getMimeType(contentType, filename))
                .setPrimaryDocument(primaryDocument);

        try {
            cryptoMessagePersister.writeStream(message.getConversationId(), file.getIdentifier(), inputStream, size);
        } catch (IOException e) {
            throw new MessagePersistException(filename);
        }

        files.add(file);

        messageRepo.save(message);
    }

    private String getMimeType(String contentType, String filename) {
        if (MediaType.APPLICATION_OCTET_STREAM_VALUE.equals(contentType)) {
            String ext = Stream.of(filename.split(".")).reduce((a, b) -> b).orElse("pdf");
            return MimeTypeExtensionMapper.getMimetype(ext);
        }

        return contentType;
    }

    private boolean isPrimaryDocument(NextMoveOutMessage message, String filename) {
        return filename.equals(message.getBusinessMessage().getPrimaerDokumentNavn());
    }

    void sendMessage(NextMoveMessage message) {
        validate(message);
        internalQueue.enqueueNextMove2(message);
    }

    private void validate(NextMoveMessage message) {
        // Must always be atleast one attachment
        if (message.getFiles() == null || message.getFiles().isEmpty()) {
            throw new MissingFileException();
        }

        if (ServiceIdentifier.DPO == message.getServiceIdentifier()) {
            // Verify each file referenced in arkivmelding is uploaded
            List<String> arkivmeldingFiles;
            try {
                arkivmeldingFiles = ArkivmeldingUtil.getFilenames(getArkivmelding(message));
            } catch (ArkivmeldingException e) {
                throw new ArkivmeldingProcessingException(e);
            }
            Set<String> messageFiles = message.getFiles().stream()
                    .map(BusinessMessageFile::getFilename)
                    .collect(Collectors.toSet());

            List<String> missingFiles = arkivmeldingFiles.stream()
                    .filter(p -> !messageFiles.contains(p))
                    .collect(Collectors.toList());

            if (!missingFiles.isEmpty()) {
                throw new MissingArkivmeldingFileException(String.join(",", missingFiles));
            }
        }
    }

    private Arkivmelding getArkivmelding(NextMoveMessage message) {
        // Arkivmelding must exist for DPO
        BusinessMessageFile arkivmeldingFile = message.getFiles().stream()
                .filter(f -> NextMoveConsts.ARKIVMELDING_FILE.equals(f.getFilename()))
                .findAny()
                .orElseThrow(MissingArkivmeldingException::new);

        try (FileEntryStream fileEntryStream = cryptoMessagePersister.readStream(message.getConversationId(), arkivmeldingFile.getIdentifier())) {
            return ArkivmeldingUtil.unmarshalArkivmelding(fileEntryStream.getInputStream());
        } catch (JAXBException | IOException e) {
            throw new UnmarshalArkivmeldingException();
        }
    }

    private String createConversationId() {
        return uuidGenerator.generate();
    }
}
