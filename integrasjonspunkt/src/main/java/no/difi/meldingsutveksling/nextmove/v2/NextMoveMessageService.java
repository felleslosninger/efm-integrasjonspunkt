package no.difi.meldingsutveksling.nextmove.v2;

import com.querydsl.core.types.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.arkivverket.standarder.noark5.arkivmelding.Arkivmelding;
import no.arkivverket.standarder.noark5.arkivmelding.Journalpost;
import no.arkivverket.standarder.noark5.arkivmelding.Saksmappe;
import no.difi.meldingsutveksling.MimeTypeExtensionMapper;
import no.difi.meldingsutveksling.NextMoveConsts;
import no.difi.meldingsutveksling.api.ConversationService;
import no.difi.meldingsutveksling.api.MessagePersister;
import no.difi.meldingsutveksling.api.OptionalCryptoMessagePersister;
import no.difi.meldingsutveksling.arkivmelding.ArkivmeldingUtil;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.exceptions.MessageNotFoundException;
import no.difi.meldingsutveksling.exceptions.MessagePersistException;
import no.difi.meldingsutveksling.exceptions.TimeToLiveException;
import no.difi.meldingsutveksling.nextmove.BusinessMessageFile;
import no.difi.meldingsutveksling.nextmove.NextMoveOutMessage;
import no.difi.meldingsutveksling.nextmove.NextMoveRuntimeException;
import no.difi.meldingsutveksling.nextmove.InternalQueue;
import no.difi.meldingsutveksling.status.Conversation;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static no.difi.meldingsutveksling.NextMoveConsts.ARKIVMELDING_FILE;

@Component
@Slf4j
@RequiredArgsConstructor
public class NextMoveMessageService {

    private final NextMoveValidator validator;
    private final NextMoveOutMessageFactory nextMoveOutMessageFactory;
    private final OptionalCryptoMessagePersister optionalCryptoMessagePersister;
    private final NextMoveMessageOutRepository messageRepo;
    private final InternalQueue internalQueue;
    private final ConversationService conversationService;
    private final ArkivmeldingUtil arkivmeldingUtil;
    private final MessagePersister messagePersister;
    private final BusinessMessageFileRepository businessMessageFileRepository;

    @Transactional(readOnly = true)
    public NextMoveOutMessage getMessage(String messageId) {
        return messageRepo.findByMessageId(messageId)
                .orElseThrow(() -> new MessageNotFoundException(messageId));
    }

    public Page<NextMoveOutMessage> findMessages(Predicate predicate, Pageable pageable) {
        return messageRepo.findAll(predicate, pageable);
    }

    @Transactional(noRollbackFor = TimeToLiveException.class)
    public NextMoveOutMessage createMessage(StandardBusinessDocument sbd, List<? extends MultipartFile> files, boolean print) {
        NextMoveOutMessage message = createMessage(sbd, print);
        files.forEach(file -> addFile(message, file));
        return message;
    }

    public NextMoveOutMessage createMessage(StandardBusinessDocument sbd, boolean print) {
        validator.validate(sbd, print);
        NextMoveOutMessage message = nextMoveOutMessageFactory.getNextMoveOutMessage(sbd, print);
        MDC.put(NextMoveConsts.CORRELATION_ID, message.getMessageId());
        messageRepo.save(message);
        conversationService.registerConversation(message);
        return message;
    }

    @Transactional
    public void deleteMessage(String messageId) {
        try {
            messagePersister.delete(messageId);
        } catch (IOException e) {
            log.error("Error deleting files from message with id={}", messageId, e);
        }

        messageRepo.findIdByMessageId(messageId).ifPresent(
                id -> {
                    businessMessageFileRepository.deleteFilesByMessageId(id);
                    messageRepo.deleteMessageById(id);
                }
        );
    }

    public void addFile(NextMoveOutMessage message, MultipartFile file) {
        validator.validateFile(message, file);

        String identifier = persistFile(message, file);

        message.addFile(new BusinessMessageFile()
                .setIdentifier(identifier)
                .setTitle(getTitle(file.getName()))
                .setFilename(file.getOriginalFilename())
                .setSize(file.getSize())
                .setMimetype(getMimeType(file.getContentType(), file.getOriginalFilename()))
                .setPrimaryDocument(message.isPrimaryDocument(file.getOriginalFilename())));

        if (ARKIVMELDING_FILE.equals(file.getOriginalFilename())) {
            Arkivmelding arkivmelding = getArkivmelding(message, identifier);
            Journalpost journalpost = arkivmeldingUtil.getJournalpost(arkivmelding);
            Saksmappe saksmappe = arkivmeldingUtil.getSaksmappe(arkivmelding);
            Optional<Conversation> conversation = conversationService.findConversation(message.getMessageId());
            conversation.ifPresent(c -> {
                c.setMessageTitle(journalpost.getOffentligTittel());
                if (journalpost.getJournalpostnummer() != null) {
                    c.setMessageReference(saksmappe.getSystemID() + "-" + journalpost.getJournalpostnummer().toString());
                }
                conversationService.save(c);
            });
        }

        messageRepo.save(message);
    }

    private Arkivmelding getArkivmelding(NextMoveOutMessage message, String identifier) {
        try (InputStream is = new ByteArrayInputStream(optionalCryptoMessagePersister.read(message.getMessageId(), identifier))) {
            return arkivmeldingUtil.unmarshalArkivmelding(is);
        } catch (JAXBException | IOException e) {
            throw new NextMoveRuntimeException("Failed to get Arkivmelding", e);
        }
    }

    private String getTitle(String name) {
        return StringUtils.hasText(name) ? name : null;
    }

    private String persistFile(NextMoveOutMessage message, MultipartFile file) {
        String identifier = UUID.randomUUID().toString();

        try {
            optionalCryptoMessagePersister.writeStream(message.getMessageId(), identifier, file.getInputStream());
        } catch (IOException e) {
            throw new MessagePersistException(file.getOriginalFilename());
        }

        return identifier;
    }

    private String getMimeType(String contentType, String filename) {
        if (MediaType.APPLICATION_OCTET_STREAM_VALUE.equals(contentType)) {
            String ext = Stream.of(filename.split("\\.")).reduce((a, b) -> b).orElse("pdf");
            return MimeTypeExtensionMapper.getMimetype(ext);
        }

        return contentType;
    }

    @Transactional(noRollbackFor = TimeToLiveException.class)
    public void sendMessage(NextMoveOutMessage message) {
        validator.validate(message);
        internalQueue.enqueueNextMove(message);
    }

    @Transactional(noRollbackFor = TimeToLiveException.class)
    public void sendMessage(Long id) {
        messageRepo.findById(id).ifPresent(this::sendMessage);
    }
}
