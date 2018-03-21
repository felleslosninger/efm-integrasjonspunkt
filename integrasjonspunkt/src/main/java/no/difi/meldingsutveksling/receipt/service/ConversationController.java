package no.difi.meldingsutveksling.receipt.service;

import com.google.common.collect.Lists;
import com.querydsl.core.BooleanBuilder;
import io.swagger.annotations.*;
import no.difi.meldingsutveksling.receipt.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static no.difi.meldingsutveksling.nextmove.ConversationDirection.INCOMING;
import static no.difi.meldingsutveksling.nextmove.ConversationDirection.OUTGOING;

@RestController
@Api
public class ConversationController {

    @Autowired
    private ConversationRepository convoRepo;

    @Autowired
    private MessageStatusRepository statusRepo;

    @RequestMapping(value = "/conversations", method = RequestMethod.GET)
    @ApiOperation(value = "Get all conversations", notes = "Gets a list of all outgoing conversations")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success", response = Conversation[].class)
    })
    public List<Conversation> conversations(
            @ApiParam(value = "Filter conversations based on finished status")
            @RequestParam(value = "finished", required = false) Optional<Boolean> finished,
            @ApiParam(value = "Filter conversations based on receiver identifier")
            @RequestParam(value = "receiverIdentifier", required = false) Optional<String> receiverIdentifier,
            @ApiParam(value = "Filter conversations based on having given status")
            @RequestParam(value = "status", required = false) Optional<String> status) {

        QConversation c = QConversation.conversation;
        BooleanBuilder p = new BooleanBuilder();

        p.and(c.direction.eq(OUTGOING));
        finished.ifPresent(f -> p.and(c.finished.eq(f)));
        receiverIdentifier.ifPresent(r -> p.and(c.receiverIdentifier.eq(r)));
        status.ifPresent(s -> p.and(c.messageStatuses.any().status.equalsIgnoreCase(s)));

        return Lists.newArrayList(convoRepo.findAll(p, c.lastUpdate.desc()));
    }

    @RequestMapping(value = "/in/conversations", method = RequestMethod.GET)
    @ApiOperation(value = "Get all conversations", notes = "Gets a list of all incoming conversations")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success", response = Conversation[].class)
    })
    public List<Conversation> incomingConversations() {
        ArrayList<Conversation> conversations = Lists.newArrayList(convoRepo.findByDirection(INCOMING));
        return conversations.stream().sorted((a, b) -> b.getLastUpdate().compareTo(a.getLastUpdate())).collect(Collectors.toList());
    }

    @RequestMapping(value = "/conversations/{id}", method = RequestMethod.GET)
    @ApiOperation(value = "Find conversation", notes = "Find conversation based on id")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success", response = Conversation.class)
    })
    public ResponseEntity conversation(
            @ApiParam(value = "convId", required = true)
            @PathVariable("id") String id,
            @ApiParam(value = "Use conversationId for search")
            @RequestParam(value = "useConversationId", required = false) boolean useConversationId) {

        Optional<Conversation> c;
        if (useConversationId) {
            c = convoRepo.findByConversationIdAndDirection(id, OUTGOING).stream().findFirst();
        } else {
            if (!StringUtils.isNumeric(id)) {
                return ResponseEntity.badRequest().body("convId is not numeric");
            }
            c = convoRepo.findByConvIdAndDirection(Integer.valueOf(id), OUTGOING);
        }

        if (!c.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(c.get());
    }

    @RequestMapping(value = "/conversations/queue", method = RequestMethod.GET)
    @ApiOperation(value = "Queued conversations", notes = "Get all conversations with not-finished state")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success", response = Conversation[].class)
    })
    public List<Conversation> queuedConversations() {
        return Lists.newArrayList(convoRepo.findByPollable(true));
    }

    @RequestMapping(value = "/statuses", method = RequestMethod.GET)
    @ApiOperation(value = "Get all statuses", notes = "Get a list of all statuses with given parameters")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success", response = MessageStatus[].class)
    })
    public ResponseEntity statuses(
            @ApiParam(value = "Get all statuses with id equals to given value and higher")
            @RequestParam(value = "fromId", required = false) Optional<Integer> fromId,
            @ApiParam(value = "Get all statuses with given convId")
            @RequestParam(value = "convId", required = false) Optional<Integer> convId) {

        QMessageStatus ms = QMessageStatus.messageStatus;
        BooleanBuilder p = new BooleanBuilder();

        fromId.ifPresent(id -> p.and(ms.statId.goe(id)));
        convId.ifPresent(id -> p.and(ms.convId.eq(id)));

        return ResponseEntity.ok(statusRepo.findAll(p, ms.statId.asc()));

    }

    @RequestMapping(value = "/statuses/{id}", method = RequestMethod.GET)
    @ApiOperation(value = "Find status", notes = "Find status with given id")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success", response = MessageStatus.class)
    })
    public ResponseEntity status(
            @ApiParam(value = "Status id", required = true)
            @PathVariable("id") Integer id) {

        Optional<MessageStatus> r = statusRepo.findByStatId(id);
        if (!r.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(r.get());
    }

    @RequestMapping(value = "/statuses/peek", method = RequestMethod.GET)
    @ApiOperation(value = "Latest status", notes = "Get status with latest update")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success", response = MessageStatus.class)
    })
    public ResponseEntity statusPeek() {
        Optional<MessageStatus> r = statusRepo.findFirstByOrderByLastUpdateAsc();
        if (r.isPresent()) {
            return ResponseEntity.ok(r.get());
        }

        return ResponseEntity.noContent().build();
    }


}
