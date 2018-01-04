package no.difi.meldingsutveksling.receipt.service;

import com.google.common.collect.Lists;
import io.swagger.annotations.*;
import no.difi.meldingsutveksling.receipt.Conversation;
import no.difi.meldingsutveksling.receipt.ConversationRepository;
import no.difi.meldingsutveksling.receipt.MessageStatus;
import no.difi.meldingsutveksling.receipt.MessageStatusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static no.difi.meldingsutveksling.nextmove.ConversationDirection.OUTGOING;

@RestController
@Api
public class ConversationController {

    @Autowired
    private ConversationRepository convoRepo;

    @Autowired
    private MessageStatusRepository statusRepo;

    @RequestMapping(value = "/conversations", method = RequestMethod.GET)
    @ApiOperation(value = "Get all conversations", notes = "Gets a list of all conversations")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success", response = Conversation[].class)
    })
    public List<Conversation> conversations(
            @ApiParam(value = "Filter conversations based on finished status")
            @RequestParam(value = "finished", required = false) Optional<Boolean> finished) {

        List<Conversation> conversations;
        if (finished.isPresent()) {
            conversations = convoRepo.findByFinishedAndDirection(finished.get(), OUTGOING);
        } else {
            conversations = Lists.newArrayList(convoRepo.findByDirection(OUTGOING));
        }
        return conversations.stream().sorted((a, b) -> b.getLastUpdate().compareTo(a.getLastUpdate())).collect(Collectors.toList());
    }

    @RequestMapping(value = "/conversations/{id}", method = RequestMethod.GET)
    @ApiOperation(value = "Find conversation", notes = "Find conversation based on id")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success", response = Conversation.class)
    })
    public ResponseEntity conversation(
            @ApiParam(value = "Conversation id", required = true)
            @PathVariable("id") Integer id) {

        Optional<Conversation> c = convoRepo.findByConvIdAndDirection(id, OUTGOING);
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
            @RequestParam(value = "fromId", required = false) Integer fromId,
            @ApiParam(value = "Get all statuses with given convId")
            @RequestParam(value = "convId", required = false) Integer convId) {

        List<MessageStatus> statuses;
        if (fromId != null) {
            if (convId != null) {
                statuses = statusRepo.findAllByConvIdAndStatIdGreaterThanEqual(convId, fromId);
            } else {
                statuses = statusRepo.findByStatIdGreaterThanEqual(fromId);
            }
        } else {
            if (convId != null) {
                statuses = statusRepo.findAllByConvId(convId);
            } else {
                statuses = Lists.newArrayList(statusRepo.findAll());
            }
        }

        Stream<MessageStatus> s = StreamSupport.stream(statuses.spliterator(), false);
        List<MessageStatus> sorted = s.sorted(Comparator.comparingInt(r -> r.getStatId())).collect(Collectors.toList());
        return ResponseEntity.ok(sorted);
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
