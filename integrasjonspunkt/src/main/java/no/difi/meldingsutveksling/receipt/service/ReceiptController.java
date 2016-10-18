package no.difi.meldingsutveksling.receipt.service;

import com.google.common.collect.Lists;
import no.difi.meldingsutveksling.receipt.MessageReceipt;
import no.difi.meldingsutveksling.receipt.MessageReceiptRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ReceiptController {

    @Autowired
    private MessageReceiptRepository repo;

    @RequestMapping("/receipts")
    public List<MessageReceipt> receipts() {
        return Lists.newArrayList(repo.findAll());
    }

    @RequestMapping("/receipts/{id}")
    public MessageReceipt receipt(@PathVariable("id") String id) {
        return repo.findOne(id);
    }

    @RequestMapping("/receipts/queue")
    public List<MessageReceipt> queuedReceipts() {
        return Lists.newArrayList(repo.findByReceived(false));
    }
}
