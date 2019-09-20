package no.difi.meldingsutveksling.receipt.service;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.annotations.*;
import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.exceptions.MessageNotFoundException;
import no.difi.meldingsutveksling.receipt.Conversation;
import no.difi.meldingsutveksling.receipt.ConversationQueryInput;
import no.difi.meldingsutveksling.receipt.ConversationRepository;
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

import static no.difi.meldingsutveksling.nextmove.ConversationDirection.OUTGOING;

@RestController
@Validated
@Api(tags = "Conversations")
@RequiredArgsConstructor
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationRepository convoRepo;

    @GetMapping
    @ApiOperation(
            value = "Get all conversations",
            notes = "Gets a list of all outgoing conversations")
    @ApiResponses({
            @ApiResponse(code = 400, message = "Bad Request", response = String.class),
            @ApiResponse(code = 200, message = "Success", response = Conversation[].class),
    })
    @JsonView(Views.Conversation.class)
    public Page<Conversation> conversations(
            @Valid ConversationQueryInput input,
            @PageableDefault(sort = "lastUpdate", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return convoRepo.findWithMessageStatuses(input, pageable);
    }

    @GetMapping("{id}")
    @ApiOperation(value = "Get conversation", notes = "Find conversation based on id")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success", response = Conversation.class),
            @ApiResponse(code = 400, message = "Bad Request", response = String.class),
            @ApiResponse(code = 404, message = "Not Found", response = String.class)
    })
    @JsonView(Views.Conversation.class)
    public Conversation getById(
            @ApiParam(value = "id", required = true, example = "1")
            @PathVariable("id") Long id) {
        return convoRepo.findByIdAndDirection(id, OUTGOING)
                .orElseThrow(() -> new MessageNotFoundException("id", id.toString()));
    }

    @GetMapping("messageId/{id}")
    @ApiOperation(value = "Get conversation", notes = "Find conversation based on messageId")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success", response = Conversation.class),
            @ApiResponse(code = 400, message = "Bad Request", response = String.class),
            @ApiResponse(code = 404, message = "Not Found", response = String.class)
    })
    @JsonView(Views.Conversation.class)
    public Conversation getByMessageId(
            @ApiParam(value = "messageId", required = true)
            @PathVariable("id") String messageId) {
        return convoRepo.findByMessageIdAndDirection(messageId, OUTGOING)
                .stream()
                .findFirst()
                .orElseThrow(() -> new MessageNotFoundException(messageId));
    }

    @GetMapping("queue")
    @ApiOperation(value = "Queued conversations", notes = "Get all conversations with not-finished state")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success", response = Conversation[].class)
    })
    @JsonView(Views.Conversation.class)
    public Page<Conversation> queuedConversations(@PageableDefault(sort = "lastUpdate", direction = Sort.Direction.DESC) Pageable pageable) {
        return convoRepo.findByPollable(true, pageable);
    }
}
