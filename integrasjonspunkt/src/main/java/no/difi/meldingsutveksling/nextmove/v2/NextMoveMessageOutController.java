package no.difi.meldingsutveksling.nextmove.v2;

import com.querydsl.core.types.Predicate;
import io.swagger.annotations.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.exceptions.DuplicateFilenameException;
import no.difi.meldingsutveksling.exceptions.MultipartFileToLargeException;
import no.difi.meldingsutveksling.exceptions.TimeToLiveException;
import no.difi.meldingsutveksling.nextmove.NextMoveOutMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartRequest;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@Validated
@Api(tags = "Outgoing messages")
@RequestMapping("/api/messages/out")
@Slf4j
@RequiredArgsConstructor
public class NextMoveMessageOutController {

    private static final int MAX_SIZE = 5 * 1024 * 1024;

    private final NextMoveMessageService messageService;

    @PostMapping(value = "multipart", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ApiOperation(value = "Create and send multipart message", notes = "Create and send a new message with the given values")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success", response = StandardBusinessDocument.class),
            @ApiResponse(code = 400, message = "Bad request", response = String.class)
    })
    public StandardBusinessDocument createAndSendMessage(
            @ApiParam(name = "SBD", value = "Standard Business Document to send. Please note that the property name is not 'any'. \n"
                    + "It is one of the following: arkivmelding, digital, digital_dpv, print, innsynskrav or publisering.", required = true)
            @RequestParam("sbd") @NotNull @Valid StandardBusinessDocument sbd,
            MultipartRequest multipartRequest) {
        List<MultipartFile> files = multipartRequest.getMultiFileMap().values().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        // Check for max size
        files.stream()
                .filter(p -> p.getSize() > MAX_SIZE)
                .findAny()
                .ifPresent(p -> {
                    throw new MultipartFileToLargeException(p.getOriginalFilename(), MAX_SIZE);
                });
        // Check for duplicate filenames
        List<String> filenames = files.stream()
                .map(MultipartFile::getOriginalFilename)
                .collect(Collectors.toList());
        filenames.stream()
                .filter(f -> Collections.frequency(filenames, f) > 1)
                .reduce((a, b) -> a+", "+b)
                .ifPresent(d -> {
                    throw new DuplicateFilenameException(d);
                });

        NextMoveOutMessage message = messageService.createMessage(sbd, files);
        messageService.sendMessage(message.getId());
        return message.getSbd();
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ApiOperation(value = "Create message", notes = "Create a new message with the given values")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success", response = StandardBusinessDocument.class),
            @ApiResponse(code = 400, message = "Bad request", response = String.class)
    })
    @Transactional(noRollbackFor = TimeToLiveException.class)
    public StandardBusinessDocument createMessage(
            @ApiParam(name = "SBD", value = "Standard Business Document to send. Please note that the property name is not 'any'. \n"
                    + "It is one of the following: arkivmelding, digital, digital_dpv, print, innsynskrav or publisering.", required = true)
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
                .map(NextMoveOutMessage::getSbd);
    }

    @GetMapping("/{messageId}")
    @ApiOperation(value = "Get message", notes = "Returns message with given messageId")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success", response = StandardBusinessDocument.class),
            @ApiResponse(code = 400, message = "Bad request", response = String.class)
    })
    @Transactional
    public StandardBusinessDocument getMessage(
            @ApiParam(
                    value = "The message ID (UUID)",
                    example = "90c0bacf-c233-4a54-96fc-e205b79862d9",
                    required = true
            )
            @PathVariable("messageId") String messageId) {
        return messageService.getMessage(messageId).getSbd();
    }

    @DeleteMapping("/{messageId}")
    @ApiOperation(value = "Delete message", notes = "Delete a message with given messageId. Also deletes all associated files.")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success", response = StandardBusinessDocument.class),
            @ApiResponse(code = 400, message = "Bad request", response = String.class)
    })
    @Transactional
    public void deleteMessage(
            @ApiParam(
                    value = "The message ID (UUID)",
                    example = "90c0bacf-c233-4a54-96fc-e205b79862d9",
                    required = true
            )
            @PathVariable("messageId") String messageId) {
        messageService.deleteMessage(messageId);
    }

    @PutMapping(value = "/{messageId}")
    @ApiOperation(value = "Upload file", notes = "Upload a file to the message with supplied messageId")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 400, message = "Bad request")
    })
    @Transactional
    public void uploadFile(
            @ApiParam(
                    value = "The message ID. Usually a UUID",
                    example = "90c0bacf-c233-4a54-96fc-e205b79862d9",
                    required = true
            )
            @PathVariable("messageId") String messageId,
            @ApiParam(
                    value = "HTTP header",
                    example = "application/pdf"
            )
            @RequestHeader(HttpHeaders.CONTENT_TYPE) String contentType,
            @ApiParam(
                    value = "HTTP header",
                    example = "attachment; name=\"The title\"; filename=\"filename.pdf\""
            )
            @RequestHeader(HttpHeaders.CONTENT_DISPOSITION) String contentDisposition,
            @ApiParam(
                    value = "The title of the document. The title can alternatively be specified using the name attribute of the Content-Disposition header.",
                    example = "My nice and shiny document title"
            )
            @RequestParam(value = "title", required = false) String title,
            HttpServletRequest request) {
        NextMoveOutMessage message = messageService.getMessage(messageId);
        messageService.addFile(message, new NextMoveUploadedFile(contentType, contentDisposition, title, request));
    }

    @PostMapping("/{messageId}")
    @ApiOperation(value = "Send message", notes = "Send the message with supplied messageId")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success", response = StandardBusinessDocument.class),
            @ApiResponse(code = 400, message = "Bad request", response = String.class)
    })
    public void sendMessage(
            @ApiParam(
                    value = "The message ID. Usually a UUID",
                    example = "90c0bacf-c233-4a54-96fc-e205b79862d9",
                    required = true
            )
            @PathVariable("messageId") String messageId) {
        NextMoveOutMessage message = messageService.getMessage(messageId);
        messageService.sendMessage(message);
    }
}
