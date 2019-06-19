package no.difi.meldingsutveksling.receipt.service;

import io.swagger.annotations.*;
import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.exceptions.ConversationNotFoundException;
import no.difi.meldingsutveksling.receipt.Conversation;
import no.difi.meldingsutveksling.receipt.ConversationQueryInput;
import no.difi.meldingsutveksling.receipt.ConversationRepository;
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
    public Page<Conversation> conversations(
            @Valid ConversationQueryInput input,
            @PageableDefault(sort = "lastUpdate", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return convoRepo.find(input, pageable);
    }

    @GetMapping("{id}")
    @ApiOperation(value = "Get conversation", notes = "Find conversation based on id")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success", response = Conversation.class),
            @ApiResponse(code = 400, message = "Bad Request", response = String.class),
            @ApiResponse(code = 404, message = "Not Found", response = String.class)
    })
    public Conversation getByConvId(
            @ApiParam(value = "convId", required = true, example = "1")
            @PathVariable("id") Integer convId) {
        return convoRepo.findByConvIdAndDirection(convId, OUTGOING)
                .orElseThrow(() -> new ConversationNotFoundException("convId", convId.toString()));
    }

    @GetMapping("conversationId/{id}")
    @ApiOperation(value = "Get conversation", notes = "Find conversation based on conversationId")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success", response = Conversation.class),
            @ApiResponse(code = 400, message = "Bad Request", response = String.class),
            @ApiResponse(code = 404, message = "Not Found", response = String.class)
    })
    public Conversation getByConversationId(
            @ApiParam(value = "conversationId", required = true)
            @PathVariable("id") String conversationId) {
        return convoRepo.findByConversationIdAndDirection(conversationId, OUTGOING)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));
    }

    @GetMapping("queue")
    @ApiOperation(value = "Queued conversations", notes = "Get all conversations with not-finished state")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success", response = Conversation[].class)
    })
    public Page<Conversation> queuedConversations(@PageableDefault(sort = "lastUpdate", direction = Sort.Direction.DESC) Pageable pageable) {
        return convoRepo.findByPollable(true, pageable);
    }
}
