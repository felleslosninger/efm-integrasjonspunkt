package no.difi.meldingsutveksling.receipt.service;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.annotations.*;
import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.exceptions.NoContentException;
import no.difi.meldingsutveksling.nextmove.NextMoveRuntimeException;
import no.difi.meldingsutveksling.receipt.MessageStatus;
import no.difi.meldingsutveksling.receipt.MessageStatusQueryInput;
import no.difi.meldingsutveksling.receipt.MessageStatusRepository;
import no.difi.meldingsutveksling.receipt.StatusQueue;
import no.difi.meldingsutveksling.view.Views;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Optional;

import static java.lang.String.format;

@RestController
@Validated
@Api(tags = "Message status")
@RequiredArgsConstructor
@RequestMapping("/api/statuses")
public class MessageStatusController {

    private final MessageStatusRepository statusRepo;
    private final StatusQueue statusQueue;

    @GetMapping
    @ApiOperation(value = "Get all statuses", notes = "Get a list of all statuses with given parameters")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success", response = MessageStatus[].class),
            @ApiResponse(code = 400, message = "Bad Request", response = String.class)
    })
    @JsonView(Views.MessageStatus.class)
    public Page<MessageStatus> find(
            @Valid MessageStatusQueryInput input,
            @PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return statusRepo.find(input, pageable);
    }

    @GetMapping("{messageId}")
    @ApiOperation(value = "Get all statuses for a given messageId", notes = "Get all statuses for a given messageId")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success", response = MessageStatus[].class),
            @ApiResponse(code = 400, message = "Bad Request", response = String.class),
            @ApiResponse(code = 404, message = "Not Found", response = String.class)
    })
    @JsonView(Views.MessageStatus.class)
    public Page<MessageStatus> findByMessageId(
            @ApiParam(value = "MessageId", required = true, example = "ff88849c-e281-4809-8555-7cd54952b917")
            @PathVariable("messageId") String messageId,
            @PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {

        return statusRepo.findByConversationMessageId(messageId, pageable);
    }

    @GetMapping("peek")
    @ApiOperation(value = "Latest status", notes = "Get status with latest update")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success", response = MessageStatus.class),
            @ApiResponse(code = 204, message = "No Content", response = String.class)
    })
    @JsonView(Views.MessageStatus.class)
    public MessageStatus peekLatest() {
        Optional<Long> statusId = statusQueue.receiveStatus();
        return statusId.map(s -> statusRepo.findById(s)
                    .orElseThrow(() -> new NextMoveRuntimeException(format("MessageStatus with id=%s not found in DB", s))))
                .orElseThrow(NoContentException::new);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> removeStatus(@PathVariable Long id) {
        if (statusQueue.removeStatus(id)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}
