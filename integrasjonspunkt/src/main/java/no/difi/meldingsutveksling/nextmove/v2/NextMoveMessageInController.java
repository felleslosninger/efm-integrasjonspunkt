package no.difi.meldingsutveksling.nextmove.v2;

import com.querydsl.core.types.Predicate;
import io.swagger.annotations.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.IntegrasjonspunktNokkel;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.dokumentpakking.service.CmsUtil;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.exceptions.ConversationNotFoundException;
import no.difi.meldingsutveksling.exceptions.ConversationNotLockedException;
import no.difi.meldingsutveksling.exceptions.FileNotFoundException;
import no.difi.meldingsutveksling.exceptions.NotContentException;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.nextmove.NextMoveInMessage;
import no.difi.meldingsutveksling.nextmove.message.FileEntryStream;
import no.difi.meldingsutveksling.nextmove.message.MessagePersister;
import no.difi.meldingsutveksling.receipt.ConversationService;
import no.difi.meldingsutveksling.receipt.GenericReceiptStatus;
import no.difi.meldingsutveksling.receipt.MessageStatus;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.persistence.PersistenceException;
import javax.transaction.Transactional;
import java.io.IOException;
import java.time.ZonedDateTime;

import static java.lang.String.format;
import static no.difi.meldingsutveksling.NextMoveConsts.ASIC_FILE;
import static no.difi.meldingsutveksling.nextmove.NextMoveMessageMarkers.markerFrom;

@RestController
@Validated
@Api
@RequestMapping("/api/message/in")
@Slf4j
@RequiredArgsConstructor
public class NextMoveMessageInController {

    private static final String HEADER_CONTENT_DISPOSITION = "Content-Disposition";
    private static final String HEADER_FILENAME = "attachement; filename=";

    private final IntegrasjonspunktProperties props;
    private final ConversationService conversationService;
    private final NextMoveMessageInRepository messageRepo;
    private final MessagePersister messagePersister;
    private final IntegrasjonspunktNokkel keyInfo;
    private final CmsUtil cmsUtil;

    @GetMapping
    @ApiOperation(value = "Get all incoming messages")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success", response = StandardBusinessDocument[].class),
            @ApiResponse(code = 404, message = "Not found", response = String.class),
            @ApiResponse(code = 204, message = "No content", response = String.class)
    })
    @Transactional
    public Page<StandardBusinessDocument> getMessages(
            @QuerydslPredicate(root = NextMoveInMessage.class) Predicate predicate,
            @PageableDefault Pageable pageable) {
        return messageRepo.findAll(predicate, pageable)
                .map(NextMoveInMessage::getSbd);
    }

    @GetMapping(value = "peek")
    @ApiOperation(value = "Peek and lock incoming queue", notes = "Gets the first message in the incoming queue, then locks the message")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success", response = StandardBusinessDocument.class),
            @ApiResponse(code = 204, message = "No content", response = String.class)
    })
    @Transactional
    public StandardBusinessDocument peek() {
        NextMoveInMessage message = messageRepo.findFirstByLockTimeoutIsNullOrderByLastUpdatedAsc()
                .orElseThrow(NotContentException::new);

        messageRepo.save(message.setLockTimeout(ZonedDateTime.now()
                .plusMinutes(props.getNextmove().getLockTimeoutMinutes())));

        log.info(markerFrom(message), "Conversation with id={} locked", message.getConversationId());
        return message.getSbd();
    }

    @GetMapping(value = "pop/{conversationId}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @ApiOperation(value = "Pop incoming queue", notes = "Gets the ASiC for the first non locked message in the queue, " +
            "unless conversationId is specified, then removes it.")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success", response = InputStreamResource.class),
            @ApiResponse(code = 204, message = "No content", response = String.class)
    })
    @Transactional
    public ResponseEntity popMessage(
            @ApiParam(value = "ConversationId", required = true)
            @PathVariable("conversationId") String conversationId) {

        NextMoveInMessage message = messageRepo.findByConversationId(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));

        if (message.getLockTimeout() != null) {
            throw new ConversationNotLockedException(conversationId);
        }

        try (FileEntryStream fileEntry = messagePersister.readStream(conversationId, ASIC_FILE)) {
            return ResponseEntity.ok()
                    .header(HEADER_CONTENT_DISPOSITION, HEADER_FILENAME + ASIC_FILE)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(fileEntry.getSize())
                    .body(new InputStreamResource(cmsUtil.decryptCMSStreamed(fileEntry.getInputStream(), keyInfo.loadPrivateKey())));
        } catch (PersistenceException | IOException e) {
            Audit.error(String.format("Can not read file \"%s\" for message [conversationId=%s, sender=%s]. Removing message from queue",
                    ASIC_FILE, message.getConversationId(), message.getSenderIdentifier()), markerFrom(message), e);
            throw new FileNotFoundException(ASIC_FILE);
        }
    }

    @DeleteMapping(value = "pop/{conversationId}")
    @ApiOperation(value = "Remove message", notes = "Delete message")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success", response = StandardBusinessDocument.class),
            @ApiResponse(code = 404, message = "Not Found", response = String.class)
    })
    @Transactional
    public StandardBusinessDocument deleteMessage(
            @ApiParam(value = "ConversationId", required = true)
            @PathVariable("conversationId") String conversationId) {
        NextMoveInMessage message = messageRepo.findByConversationId(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));

        if (message.getLockTimeout() == null) {
            throw new ConversationNotLockedException(conversationId);
        }

        try {
            messagePersister.delete(conversationId);
        } catch (IOException e) {
            log.error("Error deleting files from conversation with id={}", conversationId, e);
        }

        messageRepo.delete(message);

        conversationService.registerStatus(conversationId,
                MessageStatus.of(GenericReceiptStatus.INNKOMMENDE_LEVERT))
                .ifPresent(conversationService::markFinished);

        Audit.info(format("Conversation with id=%s popped from queue", conversationId),
                markerFrom(message));

        return message.getSbd();
    }
}
