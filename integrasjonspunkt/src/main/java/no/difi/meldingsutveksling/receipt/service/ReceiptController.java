package no.difi.meldingsutveksling.receipt.service;

import com.google.common.collect.Lists;
import no.difi.meldingsutveksling.receipt.Conversation;
import no.difi.meldingsutveksling.receipt.ConversationRepository;
import no.difi.meldingsutveksling.receipt.MessageReceipt;
import no.difi.meldingsutveksling.receipt.MessageReceiptRepository;
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
    public ResponseEntity receipt(@PathVariable("id") Integer id) {

        Optional<Conversation> c = convoRepo.findByConvId(id);
        if (!c.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(c.get());
    }

    @RequestMapping("/receipts/queue")
    public List<Conversation> queuedReceipts() {
        return Lists.newArrayList(convoRepo.findByPollable(true));
    }

    @RequestMapping("/statuses")
    public ResponseEntity statuses(
            @RequestParam(value = "fromId", required = false) Integer fromId,
            @RequestParam(value = "convId", required = false) Integer convId) {

        List<MessageReceipt> receipts;
        if (fromId != null) {
            if (convId != null) {
                receipts = receiptRepo.findAllByConvIdAndRecIdGreaterThanEqual(convId, fromId);
            } else {
                receipts = receiptRepo.findByRecIdGreaterThanEqual(fromId);
            }
        } else {
            if (convId != null) {
                receipts = receiptRepo.findAllByConvId(convId);
            } else {
                receipts = Lists.newArrayList(receiptRepo.findAll());
            }
        }

        Stream<MessageReceipt> s = StreamSupport.stream(receipts.spliterator(), false);
        List<MessageReceipt> sorted = s.sorted(Comparator.comparingInt(r -> r.getRecId())).collect(Collectors.toList());
        return ResponseEntity.ok(sorted);
    }

    @RequestMapping("/statuses/{id}")
    public ResponseEntity status(@PathVariable("id") Integer id) {

        Optional<MessageReceipt> r = receiptRepo.findByRecId(id);
        if (!r.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(r.get());
    }

    @RequestMapping("/statuses/peek")
    public ResponseEntity statusPeek() {
        Optional<MessageReceipt> r = receiptRepo.findFirstByOrderByLastUpdateAsc();
        if (r.isPresent()) {
            return ResponseEntity.ok(r.get());
        }

        return ResponseEntity.noContent().build();
    }


}
