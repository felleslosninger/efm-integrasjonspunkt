package no.difi.meldingsutveksling.nextmove.v2;

import com.querydsl.core.types.Predicate;
import io.swagger.annotations.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.exceptions.InputStreamException;
import no.difi.meldingsutveksling.exceptions.MissingHttpHeaderException;
import no.difi.meldingsutveksling.exceptions.MultipartFileToLargeException;
import no.difi.meldingsutveksling.nextmove.NextMoveMessage;
import no.difi.meldingsutveksling.nextmove.NextMoveOutMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartRequest;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;

import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@Validated
@Api
@RequestMapping("/api/message/out")
@Slf4j
@RequiredArgsConstructor
public class NextMoveMessageOutController {

    private static final int MAX_SIZE = 5 * 1024 * 1024;
    private final NextMoveMessageService messageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ApiOperation(value = "Create message", notes = "Create and send a new message with the given values")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success", response = StandardBusinessDocument.class),
            @ApiResponse(code = 400, message = "Bad request", response = String.class)
    })
    @Transactional
    public StandardBusinessDocument createAndSendMessage(
            @RequestParam("sbd") @NotNull @Valid StandardBusinessDocument sbd,
            MultipartRequest multipartRequest) {
        List<MultipartFile> files = multipartRequest.getMultiFileMap().values().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        NextMoveOutMessage message = messageService.createMessage(sbd);

        files.stream()
                .filter(p -> p.getSize() > MAX_SIZE)
                .findAny()
                .ifPresent(p -> {
                    throw new MultipartFileToLargeException(p.getOriginalFilename(), MAX_SIZE);
                });

        files.forEach(file -> messageService.addFile(message, file));

        messageService.sendMessage(message);

        return message.getSbd();
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ApiOperation(value = "Create message", notes = "Create a new message with the given values")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success", response = StandardBusinessDocument.class),
            @ApiResponse(code = 400, message = "Bad request", response = String.class)
    })
    @Transactional
    public StandardBusinessDocument createMessage(
            @Valid @RequestBody StandardBusinessDocument sbd) {
        NextMoveOutMessage message = messageService.createMessage(sbd);
        return message.getSbd();
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
        return messageService.findMessages(predicate, pageable)
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
        return messageService.getMessage(conversationId).getSbd();
    }

    @PostMapping(value = "/{conversationId}/upload")
    @ApiOperation(value = "Upload file", notes = "Upload a file to the message with supplied conversationId")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 400, message = "Bad request")
    })
    @Transactional
    public void uploadFile(
            @ApiParam(value = "ConversationId", required = true)
            @PathVariable("conversationId") String conversationId,
            @ApiParam(value = "Title", required = true)
            @RequestParam(value = "title", required = false) String title,
            HttpServletRequest request) {

        NextMoveOutMessage message = messageService.getMessage(conversationId);

        ContentDisposition contentDisposition = Optional.ofNullable(request.getHeader(HttpHeaders.CONTENT_DISPOSITION))
                .map(ContentDisposition::parse)
                .orElseThrow(() -> new MissingHttpHeaderException(HttpHeaders.CONTENT_DISPOSITION));

        try {
            messageService.addFile(
                    message,
                    title,
                    contentDisposition.getFilename(),
                    request.getContentType(),
                    request.getInputStream(),
                    request.getContentLength());
        } catch (IOException e) {
            throw new InputStreamException(contentDisposition.getFilename());
        }
    }

    @PostMapping("/{conversationId}")
    @ApiOperation(value = "Send message", notes = "Send the message with supplied conversationId")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success", response = StandardBusinessDocument.class),
            @ApiResponse(code = 400, message = "Bad request", response = String.class)
    })
    @Transactional
    public void sendMessage(@PathVariable("conversationId") String conversationId) {
        NextMoveMessage message = messageService.getMessage(conversationId);
        messageService.sendMessage(message);
    }
}
