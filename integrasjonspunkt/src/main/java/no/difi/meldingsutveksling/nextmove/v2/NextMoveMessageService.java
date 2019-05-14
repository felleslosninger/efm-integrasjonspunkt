package no.difi.meldingsutveksling.nextmove.v2;

import com.querydsl.core.types.Predicate;
import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.MimeTypeExtensionMapper;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.exceptions.ConversationNotFoundException;
import no.difi.meldingsutveksling.exceptions.MessagePersistException;
import no.difi.meldingsutveksling.nextmove.BusinessMessageFile;
import no.difi.meldingsutveksling.nextmove.NextMoveMessage;
import no.difi.meldingsutveksling.nextmove.NextMoveOutMessage;
import no.difi.meldingsutveksling.nextmove.message.CryptoMessagePersister;
import no.difi.meldingsutveksling.noarkexchange.receive.InternalQueue;
import no.difi.meldingsutveksling.receipt.ConversationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;
import java.util.stream.Stream;

import static com.google.common.base.Strings.emptyToNull;

@Component
@RequiredArgsConstructor
public class NextMoveMessageService {

    private final NextMoveValidator validator;
    private final NextMoveOutMessageFactory nextMoveOutMessageFactory;
    private final CryptoMessagePersister cryptoMessagePersister;
    private final NextMoveMessageOutRepository messageRepo;
    private final InternalQueue internalQueue;
    private final ConversationService conversationService;

    NextMoveOutMessage getMessage(String conversationId) {
        return messageRepo.findByConversationId(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));
    }

    Page<NextMoveOutMessage> findMessages(Predicate predicate, Pageable pageable) {
        return messageRepo.findAll(predicate, pageable);
    }

    NextMoveOutMessage createMessage(StandardBusinessDocument sbd) {
        validator.validate(sbd);
        NextMoveOutMessage message = nextMoveOutMessageFactory.getNextMoveOutMessage(sbd);
        messageRepo.save(message);
        conversationService.registerConversation(message);
        return message;
    }

    void addFile(NextMoveOutMessage message, MultipartFile file) {
        validator.validateFile(message, file);

        String identifier = persistFile(message, file);

        message.getOrCreateFiles().add(new BusinessMessageFile()
                .setIdentifier(identifier)
                .setTitle(emptyToNull(file.getName()))
                .setFilename(file.getOriginalFilename())
                .setMimetype(getMimeType(file.getContentType(), file.getOriginalFilename()))
                .setPrimaryDocument(message.isPrimaryDocument(file.getOriginalFilename())));

        messageRepo.save(message);
    }

    private String persistFile(NextMoveOutMessage message, MultipartFile file) {
        String identifier = UUID.randomUUID().toString();

        try {
            cryptoMessagePersister.writeStream(message.getConversationId(), identifier, file.getInputStream(), file.getSize());
        } catch (IOException e) {
            throw new MessagePersistException(file.getOriginalFilename());
        }

        return identifier;
    }

    private String getMimeType(String contentType, String filename) {
        if (MediaType.APPLICATION_OCTET_STREAM_VALUE.equals(contentType)) {
            String ext = Stream.of(filename.split(".")).reduce((a, b) -> b).orElse("pdf");
            return MimeTypeExtensionMapper.getMimetype(ext);
        }

        return contentType;
    }

    void sendMessage(NextMoveMessage message) {
        validator.validate(message);
        internalQueue.enqueueNextMove2(message);
    }
}
