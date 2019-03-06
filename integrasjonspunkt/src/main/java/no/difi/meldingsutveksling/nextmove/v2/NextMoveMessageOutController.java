package no.difi.meldingsutveksling.nextmove.v2;

import io.swagger.annotations.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.exceptions.ConversationNotFoundException;
import no.difi.meldingsutveksling.nextmove.BusinessMessageFile;
import no.difi.meldingsutveksling.nextmove.NextMoveException;
import no.difi.meldingsutveksling.nextmove.NextMoveMessage;
import no.difi.meldingsutveksling.nextmove.message.MessagePersister;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import javax.validation.Valid;
import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.google.common.base.Strings.isNullOrEmpty;

@RestController
@Validated
@Api
@RequestMapping("/api/message/out")
@Slf4j
@RequiredArgsConstructor
public class NextMoveMessageOutController {

    private final StandardBusinessDocumentRepository sbdRepo;
    private final NextMoveMessageRepository messageRepo;
    private final NextMoveMessageService messageService;
    private final MessagePersister messagePersister;

    @PostMapping
    @ApiOperation(value = "Create message", notes = "Create a new messagee with the given values")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success", response = StandardBusinessDocument.class),
            @ApiResponse(code = 400, message = "Bad request", response = String.class)
    })
    @Transactional
    public StandardBusinessDocument createMessage(
            @Valid @RequestBody StandardBusinessDocument sbd) {

        sbd = messageService.setDefaults(sbd);
        NextMoveMessage message = NextMoveMessage.of(sbd.getConversationId(), sbd.getReceiverOrgNumber(), sbd);
        messageRepo.save(message);

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
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        return sbdRepo.findAll(new PageRequest(page, size));
    }

    @GetMapping("/{conversationId}")
    @ApiOperation(value = "Get all messages", notes = "Returns all queued messages")
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

    @PostMapping("/{conversationId}")
    @ApiOperation(value = "Upload file", notes = "Upload a file to the message with supplied conversationId")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success", response = StandardBusinessDocument.class),
            @ApiResponse(code = 400, message = "Bad request", response = String.class)
    })
    @Transactional
    public ResponseEntity uploadFile(
            @ApiParam(value = "ConversationId", required = true)
            @PathVariable("conversationId") String conversationId,
            @ApiParam(value = "File name", required = true)
            @RequestParam("filename") String filename,
            @ApiParam(value = "Mimetype")
            @RequestParam(value = "mimetype", required = false) String mimetype,
            @ApiParam(value = "File title")
            @RequestParam(value = "title", required = false) String title,
            @ApiParam(value = "Flag for primary document")
            @RequestParam(value = "primaryDocument", required = false, defaultValue = "false") boolean primaryDocument,
            HttpServletRequest request) throws NextMoveException {

        NextMoveMessage message = messageRepo.findByConversationId(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));

        Set<BusinessMessageFile> files = Optional.ofNullable(message.getFiles()).orElseGet(HashSet::new);

        if (primaryDocument && files.stream().anyMatch(BusinessMessageFile::getPrimaryDocument)) {
            return ResponseEntity.badRequest().body("Messages can only contain one primary document");
        }

        try {
            messagePersister.writeStream(conversationId, filename, request.getInputStream(),
                    Long.valueOf(request.getHeader(HttpHeaders.CONTENT_LENGTH)));
        } catch (IOException e) {
            throw new NextMoveException(String.format("Could not persist file \"%s\"", filename), e);
        }

        BusinessMessageFile.BusinessMessageFileBuilder bmfBuilder = BusinessMessageFile.builder()
                .identifier(UUID.randomUUID().toString())
                .filename(filename)
                .primaryDocument(primaryDocument);
        if (!isNullOrEmpty(mimetype)) {
            bmfBuilder.mimetype(mimetype);
        }
        if (!isNullOrEmpty(title)) {
            bmfBuilder.title(title);
        }
        files.add(bmfBuilder.build());
        messageRepo.save(message);

        return ResponseEntity.ok().build();
    }
}
