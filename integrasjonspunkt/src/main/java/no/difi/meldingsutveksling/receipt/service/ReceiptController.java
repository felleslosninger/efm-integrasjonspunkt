package no.difi.meldingsutveksling.receipt.service;

import com.google.common.collect.Lists;
import no.difi.meldingsutveksling.receipt.Conversation;
import no.difi.meldingsutveksling.receipt.ConversationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ReceiptController {

    @Autowired
    private ConversationRepository repo;

    @RequestMapping("/receipts")
    public List<Conversation> receipts() {
        return Lists.newArrayList(repo.findAll());
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
