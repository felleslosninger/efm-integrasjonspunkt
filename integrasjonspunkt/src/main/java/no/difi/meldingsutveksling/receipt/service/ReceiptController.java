package no.difi.meldingsutveksling.receipt.service;

import com.google.common.collect.Lists;
import no.difi.meldingsutveksling.receipt.Conversation;
import no.difi.meldingsutveksling.receipt.ConversationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@RestController
public class ReceiptController {

    @Autowired
    private ConversationRepository repo;

    @RequestMapping("/receipts")
    public List<Conversation> receipts() {
        Stream<Conversation> s = StreamSupport.stream(repo.findAll().spliterator(), false);
        return s.sorted((a, b) -> b.getLastUpdate().compareTo(a.getLastUpdate()))
                .collect(Collectors.toList());
    }

    @RequestMapping("/receipts/{id}")
    public Conversation receipt(@PathVariable("id") String id) {
        return repo.findOne(id);
    }

    @RequestMapping("/receipts/queue")
    public List<Conversation> queuedReceipts() {
        return Lists.newArrayList(repo.findByPollable(true));
    }
}
