package no.difi.meldingsutveksling.nextmove.v2;

import com.querydsl.core.types.Predicate;
import lombok.RequiredArgsConstructor;
import no.arkivverket.standarder.noark5.arkivmelding.Arkivmelding;
import no.difi.meldingsutveksling.MimeTypeExtensionMapper;
import no.difi.meldingsutveksling.NextMoveConsts;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.arkivmelding.ArkivmeldingException;
import no.difi.meldingsutveksling.arkivmelding.ArkivmeldingUtil;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.exceptions.*;
import no.difi.meldingsutveksling.nextmove.*;
import no.difi.meldingsutveksling.nextmove.message.CryptoMessagePersister;
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

        NextMoveOutMessage message = NextMoveOutMessage.of(setDefaults(sbd));
        messageRepo.save(message);
        conversationService.registerConversation(sbd);

        return message;
    }

    private StandardBusinessDocument setDefaults(StandardBusinessDocument sbd) {
        sbd.getConversationScope().ifPresent(s -> {
            if (isNullOrEmpty(s.getInstanceIdentifier())) {
                s.setInstanceIdentifier(createConversationId());
            }
        });

        if (sbd.getServiceIdentifier() == DPI_PRINT) {
            setDpiDefaults(sbd);
        }

        return sbd;
    }

    private void setDpiDefaults(StandardBusinessDocument sbd) {
        Optional<ServiceRecord> serviceRecord = sr.getServiceRecord(sbd.getReceiverIdentifier(), DPI_PRINT);
        ServiceRecord record = serviceRecord.orElseThrow(() -> new NextMoveRuntimeException(String.format("No service record of type %s found for receiver", DPI_PRINT.toString())));
        DpiPrintMessage dpiMessage = (DpiPrintMessage) sbd.getAny();
        if (dpiMessage.getReceiver() == null) {
            dpiMessage.setReceiver(new PostAddress());
        }

        setReceiverDefaults(dpiMessage.getReceiver(), record.getPostAddress());
        setReceiverDefaults(dpiMessage.getMailReturn().getReceiver(), record.getReturnAddress());
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

        List<ServiceIdentifier> requiredTitleCapabilities = asList(DPV, DPI_DIGITAL, DPI_PRINT);
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
        return filename.equals(message.getBusinessMessage().getPrimaryDocumentFilename());
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
            // Arkivmelding must exist for DPO
            BusinessMessageFile arkivmeldingFile = message.getFiles().stream()
                    .filter(f -> NextMoveConsts.ARKIVMELDING_FILE.equals(f.getFilename()))
                    .findAny()
                    .orElseThrow(MissingArkivmeldingException::new);

            InputStream is = cryptoMessagePersister.readStream(message.getConversationId(), arkivmeldingFile.getIdentifier()).getInputStream();
            Arkivmelding arkivmelding;
            try {
                arkivmelding = ArkivmeldingUtil.unmarshalArkivmelding(is);
            } catch (JAXBException e) {
                throw new UnmarshalArkivmeldingException();
            }

            // Verify each file referenced in arkivmelding is uploaded
            List<String> arkivmeldingFiles;
            try {
                arkivmeldingFiles = ArkivmeldingUtil.getFilenames(arkivmelding);
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

    private String createConversationId() {
        return UUID.randomUUID().toString();
    }
}
