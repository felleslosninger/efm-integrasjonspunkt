package no.difi.meldingsutveksling.receipt.service;

import com.google.common.collect.Lists;
import no.difi.meldingsutveksling.receipt.Conversation;
import no.difi.meldingsutveksling.receipt.ConversationRepository;
import no.difi.meldingsutveksling.receipt.MessageReceipt;
import no.difi.meldingsutveksling.receipt.MessageReceiptRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@RestController
public class ReceiptController {

    @Autowired
    private ConversationRepository convoRepo;

    @Autowired
    private MessageReceiptRepository receiptRepo;

    @RequestMapping("/receipts")
    public List<Conversation> receipts() {
        Stream<Conversation> s = StreamSupport.stream(convoRepo.findAll().spliterator(), false);
        return s.sorted((a, b) -> b.getLastUpdate().compareTo(a.getLastUpdate())).collect(Collectors.toList());
    }

    @RequestMapping("/receipts/{id}")
    public Conversation receipt(@PathVariable("id") String id) {
        return convoRepo.findOne(id);
    }

    @RequestMapping("/receipts/queue")
    public List<Conversation> queuedReceipts() {
        return Lists.newArrayList(convoRepo.findByPollable(true));
    }

    @RequestMapping("/statuses")
    public List<MessageReceipt> statuses(
            @RequestParam(value = "fromId", required = false) Integer fromId) {

        Stream<MessageReceipt> s;
        if (fromId != null) {
            s = StreamSupport.stream(receiptRepo.findByGenIdGreaterThanEqual(fromId).spliterator(), false);
        } else {
            s = StreamSupport.stream(receiptRepo.findAll().spliterator(), false);
        }

        return s.sorted((a, b) -> b.getLastUpdate().compareTo(a.getLastUpdate())).collect(Collectors.toList());
    }


}
