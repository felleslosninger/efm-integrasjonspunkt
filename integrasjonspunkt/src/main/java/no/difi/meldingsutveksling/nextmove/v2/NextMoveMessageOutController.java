package no.difi.meldingsutveksling.nextmove.v2;

import com.querydsl.core.types.Predicate;
import io.swagger.annotations.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.MimeTypeExtensionMapper;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.exceptions.ConversationAlreadyExistsException;
import no.difi.meldingsutveksling.exceptions.ConversationNotFoundException;
import no.difi.meldingsutveksling.exceptions.MissingFileTitleException;
import no.difi.meldingsutveksling.exceptions.MultiplePrimaryDocumentsNotAllowedException;
import no.difi.meldingsutveksling.nextmove.BusinessMessageFile;
import no.difi.meldingsutveksling.nextmove.NextMoveException;
import no.difi.meldingsutveksling.nextmove.NextMoveMessage;
import no.difi.meldingsutveksling.nextmove.NextMoveOutMessage;
import no.difi.meldingsutveksling.nextmove.message.MessagePersister;
import no.difi.meldingsutveksling.noarkexchange.receive.InternalQueue;
import no.difi.meldingsutveksling.receipt.ConversationService;
import no.difi.meldingsutveksling.validation.AcceptableMimeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import javax.validation.Valid;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Strings.emptyToNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Arrays.asList;
import static no.difi.meldingsutveksling.ServiceIdentifier.DPI;
import static no.difi.meldingsutveksling.ServiceIdentifier.DPV;

@RestController
@Validated
@Api
@RequestMapping("/api/message/out")
@Slf4j
@RequiredArgsConstructor
public class NextMoveMessageOutController {

    private final NextMoveMessageOutRepository messageRepo;
    private final NextMoveMessageService messageService;
    private final MessagePersister messagePersister;
    private final InternalQueue internalQueue;
    private final ConversationService conversationService;

    @PostMapping
    @ApiOperation(value = "Create message", notes = "Create a new messagee with the given values")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success", response = StandardBusinessDocument.class),
            @ApiResponse(code = 400, message = "Bad request", response = String.class)
    })
    @Transactional
    public StandardBusinessDocument createMessage(
            @Valid @RequestBody StandardBusinessDocument sbd) {

        sbd.getOptionalConversationId()
                .flatMap(messageRepo::findByConversationId)
                .map(p -> {
                    throw new ConversationAlreadyExistsException(p.getConversationId());
                });

        NextMoveOutMessage message = NextMoveOutMessage.of(messageService.setDefaults(sbd));
        messageRepo.save(message);
        conversationService.registerConversation(sbd);

        return sbd;
    }

    @GetMapping
    @ApiOperation(value = "Get all messages", notes = "Returns all queued messages")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success", response = StandardBusinessDocument.class),
            @ApiResponse(code = 400, message = "Bad request", response = String.class)
    })
    @Transactional
    public Page<StandardBusinessDocument> getMessages(
            @QuerydslPredicate(root = NextMoveOutMessage.class) Predicate predicate,
            @PageableDefault Pageable pageable) {
        return messageRepo.findAll(predicate, pageable)
                .map(NextMoveMessage::getSbd);
    }

    @GetMapping("/{conversationId}")
    @ApiOperation(value = "Get message", notes = "Returns message with given conversationId")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success", response = StandardBusinessDocument.class),
            @ApiResponse(code = 400, message = "Bad request", response = String.class)
    })
    @Transactional
    public StandardBusinessDocument getMessage(@PathVariable("conversationId") String conversationId) {
        return messageRepo.findByConversationId(conversationId)
                .map(NextMoveMessage::getSbd)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));
    }


    @PostMapping(value = "/{conversationId}/upload", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @ApiOperation(value = "Upload file", notes = "Upload a file to the message with supplied conversationId")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 400, message = "Bad request")
    })
    @Transactional
    public void uploadFile(
            @ApiParam(value = "ConversationId", required = true)
            @PathVariable("conversationId") String conversationId,
            @ApiParam(value = "File name", required = true)
            @RequestParam("filename") String filename,
            @ApiParam(value = "Mimetype")
            @RequestParam(value = "mimetype", required = false)
            @AcceptableMimeType String mimetype,
            @ApiParam(value = "File title")
            @RequestParam(value = "title", required = false) String title,
            @ApiParam(value = "Flag for primary document")
            @RequestParam(value = "primaryDocument", required = false, defaultValue = "false") boolean primaryDocument,
            HttpServletRequest request) throws NextMoveException {

        NextMoveOutMessage message = messageRepo.findByConversationId(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));

        Set<BusinessMessageFile> files = Optional.ofNullable(message.getFiles()).orElseGet(HashSet::new);

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
                .setIdentifier(conversationId + "-" + (files.size() + 1))
                .setFilename(filename)
                .setPrimaryDocument(primaryDocument)
                .setTitle(emptyToNull(title));
        if (isNullOrEmpty(mimetype)) {
            String ext = Stream.of(filename.split(".")).reduce((a, b) -> b).orElse("pdf");
            file.setMimetype(MimeTypeExtensionMapper.getMimetype(ext));
        } else {
            file.setMimetype(mimetype);
        }

        try {
            messagePersister.writeStream(conversationId, file.getIdentifier(), request.getInputStream(),
                    Long.valueOf(request.getHeader(HttpHeaders.CONTENT_LENGTH)));
        } catch (IOException e) {
            throw new NextMoveException(String.format("Could not persist file \"%s\"", filename), e);
        }

        files.add(file);

        messageRepo.save(message);
    }

    @PostMapping("/{conversationId}")
    @ApiOperation(value = "Send message", notes = "Send the message with supplied conversationId")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success", response = StandardBusinessDocument.class),
            @ApiResponse(code = 400, message = "Bad request", response = String.class)
    })
    @Transactional
    public void sendMessage(@PathVariable("conversationId") String conversationId) {
        NextMoveMessage message = messageRepo.findByConversationId(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));
        messageService.validate(message);
        internalQueue.enqueueNextMove2(message);
    }
}
