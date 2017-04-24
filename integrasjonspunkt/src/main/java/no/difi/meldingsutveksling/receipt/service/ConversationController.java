package no.difi.meldingsutveksling.receipt.service;

import com.google.common.collect.Lists;
import no.difi.meldingsutveksling.receipt.Conversation;
import no.difi.meldingsutveksling.receipt.ConversationRepository;
import no.difi.meldingsutveksling.receipt.MessageStatus;
import no.difi.meldingsutveksling.receipt.MessageStatusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@RestController
public class ConversationController {

    @Autowired
    private ConversationRepository convoRepo;

    @Autowired
    private MessageStatusRepository statusRepo;

    @RequestMapping("/conversations")
    public List<Conversation> conversations() {
        Stream<Conversation> s = StreamSupport.stream(convoRepo.findAll().spliterator(), false);
        return s.sorted((a, b) -> b.getLastUpdate().compareTo(a.getLastUpdate())).collect(Collectors.toList());
    }

    @RequestMapping("/conversations/{id}")
    public ResponseEntity conversation(@PathVariable("id") Integer id) {

        Optional<Conversation> c = convoRepo.findByConvId(id);
        if (!c.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(c.get());
    }

    @RequestMapping("/conversations/queue")
    public List<Conversation> queuedConversations() {
        return Lists.newArrayList(convoRepo.findByPollable(true));
    }

    @RequestMapping("/statuses")
    public ResponseEntity statuses(
            @RequestParam(value = "fromId", required = false) Integer fromId,
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

    @RequestMapping("/statuses/{id}")
    public ResponseEntity status(@PathVariable("id") Integer id) {

        Optional<MessageStatus> r = statusRepo.findByStatId(id);
        if (!r.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(r.get());
    }

    @RequestMapping("/statuses/peek")
    public ResponseEntity statusPeek() {
        Optional<MessageStatus> r = statusRepo.findFirstByOrderByLastUpdateAsc();
        if (r.isPresent()) {
            return ResponseEntity.ok(r.get());
        }

        return ResponseEntity.noContent().build();
    }


}
