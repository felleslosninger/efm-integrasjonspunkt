package no.difi.meldingsutveksling.nextmove.v2;

import com.google.common.collect.Sets;
import io.swagger.annotations.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.exceptions.HttpStatusCodeException;
import no.difi.meldingsutveksling.nextmove.BusinessMessageFile;
import no.difi.meldingsutveksling.nextmove.NextMoveException;
import no.difi.meldingsutveksling.nextmove.NextMoveMessage;
import no.difi.meldingsutveksling.nextmove.message.MessagePersister;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import javax.validation.Valid;
import java.io.IOException;
import java.util.*;

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
    private final MessageSource messageSource;

    @PostMapping
    @ApiOperation(value = "Create message", notes = "Create a new messagee with the given values")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success", response = StandardBusinessDocument.class),
            @ApiResponse(code = 400, message = "Bad request", response = String.class)
    })
    @Transactional
    public StandardBusinessDocument createMessage(
            @Valid @RequestBody StandardBusinessDocument sbd) throws NextMoveException {

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
    public List<StandardBusinessDocument> getAllMessages() {
        return sbdRepo.findAll();
    }


    @GetMapping("/{conversationId}")
    @ApiOperation(value = "Get all messages", notes = "Returns all queued messages")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success", response = StandardBusinessDocument.class),
            @ApiResponse(code = 400, message = "Bad request", response = String.class)
    })
    @Transactional
    public StandardBusinessDocument getMessage(Locale locale,
                                               @PathVariable("conversationId") String conversationId) {

        return messageRepo.findByConversationId(conversationId)
                .map(NextMoveMessage::getSbd)
                .orElseThrow(() -> new HttpStatusCodeException(
                                HttpStatus.NOT_FOUND,
                                messageSource.getMessage(
                                        "no.difi.meldingsutveksling.nextmove.message.notFound",
                                        new Object[]{"conversationId", conversationId},
                                        locale)
                        )
                );
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
            @ApiParam(value = "Flag for primary odcument")
            @RequestParam(value = "primaryDocument", required = false, defaultValue = "false") boolean primaryDocument,
            HttpServletRequest request) throws NextMoveException {

        Optional<NextMoveMessage> find = messageRepo.findByConversationId(conversationId);
        if (!find.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        NextMoveMessage message = find.get();
        Set<BusinessMessageFile> files = message.getFiles();
        if (files == null) {
            files = Sets.newHashSet();
        }

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
