package no.difi.meldingsutveksling.receipt.service;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.annotations.*;
import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.exceptions.MessageStatusNotFoundException;
import no.difi.meldingsutveksling.exceptions.NoContentException;
import no.difi.meldingsutveksling.receipt.MessageStatus;
import no.difi.meldingsutveksling.receipt.MessageStatusQueryInput;
import no.difi.meldingsutveksling.receipt.MessageStatusRepository;
import no.difi.meldingsutveksling.view.Views;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@Validated
@Api(tags = "Message status")
@RequiredArgsConstructor
@RequestMapping("/api/statuses")
public class MessageStatusController {

    private final MessageStatusRepository statusRepo;

    @GetMapping
    @ApiOperation(value = "Get all statuses", notes = "Get a list of all statuses with given parameters")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success", response = MessageStatus[].class),
            @ApiResponse(code = 400, message = "Bad Request", response = String.class)
    })
    @JsonView(Views.MessageStatus.class)
    public Page<MessageStatus> statuses(
            @Valid MessageStatusQueryInput input,
            @PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return statusRepo.find(input, pageable);
    }

    @GetMapping("{messageId}")
    @ApiOperation(value = "Get status", notes = "Get status with given id")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success", response = MessageStatus.class),
            @ApiResponse(code = 400, message = "Bad Request", response = String.class),
            @ApiResponse(code = 404, message = "Not Found", response = String.class)
    })
    @JsonView(Views.MessageStatus.class)
    public MessageStatus status(
            @ApiParam(value = "MessageId", required = true, example = "ff88849c-e281-4809-8555-7cd54952b917")
            @PathVariable("messageId") String messageId) {

        return statusRepo.findByMessageId(messageId)
                .orElseThrow(() -> new MessageStatusNotFoundException(messageId));
    }

    @GetMapping("peek")
    @ApiOperation(value = "Latest status", notes = "Get status with latest update")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success", response = MessageStatus.class),
            @ApiResponse(code = 204, message = "No Content", response = String.class)
    })
    @JsonView(Views.MessageStatus.class)
    public MessageStatus statusPeek() {
        return statusRepo.findFirstByOrderByLastUpdateAsc()
                .orElseThrow(NoContentException::new);
    }
}
