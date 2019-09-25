package no.difi.meldingsutveksling.nextmove.v2;

import com.querydsl.core.types.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.MimeTypeExtensionMapper;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.exceptions.MessageNotFoundException;
import no.difi.meldingsutveksling.exceptions.MessagePersistException;
import no.difi.meldingsutveksling.exceptions.TimeToLiveException;
import no.difi.meldingsutveksling.nextmove.BusinessMessageFile;
import no.difi.meldingsutveksling.nextmove.NextMoveOutMessage;
import no.difi.meldingsutveksling.nextmove.message.OptionalCryptoMessagePersister;
import no.difi.meldingsutveksling.noarkexchange.receive.InternalQueue;
import no.difi.meldingsutveksling.receipt.ConversationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

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

    @Transactional(readOnly = true)
    public NextMoveOutMessage getMessage(String messageId) {
        return messageRepo.findByMessageId(messageId)
                .orElseThrow(() -> new MessageNotFoundException(messageId));
    }

    Page<NextMoveOutMessage> findMessages(Predicate predicate, Pageable pageable) {
        return messageRepo.findAll(predicate, pageable);
    }

    @Transactional(noRollbackFor = TimeToLiveException.class)
    public NextMoveOutMessage createMessage(StandardBusinessDocument sbd, List<? extends MultipartFile> files) {
        NextMoveOutMessage message = createMessage(sbd);
        files.forEach(file -> addFile(message, file));
        return message;
    }

    public NextMoveOutMessage createMessage(StandardBusinessDocument sbd) {
        validator.validate(sbd);
        NextMoveOutMessage message = nextMoveOutMessageFactory.getNextMoveOutMessage(sbd);
        messageRepo.save(message);
        conversationService.registerConversation(message);
        return message;
    }

    public void addFile(NextMoveOutMessage message, MultipartFile file) {
        validator.validateFile(message, file);

        String identifier = persistFile(message, file);

        message.addFile(new BusinessMessageFile()
                .setIdentifier(identifier)
                .setTitle(getTitle(file.getName()))
                .setFilename(file.getOriginalFilename())
                .setMimetype(getMimeType(file.getContentType(), file.getOriginalFilename()))
                .setPrimaryDocument(message.isPrimaryDocument(file.getOriginalFilename())));

        messageRepo.save(message);
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
