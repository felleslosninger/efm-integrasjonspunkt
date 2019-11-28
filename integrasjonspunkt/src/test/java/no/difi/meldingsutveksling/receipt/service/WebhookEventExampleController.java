package no.difi.meldingsutveksling.receipt.service;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.webhooks.event.WebhookContentBase;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class WebhookEventExampleController {

    @PostMapping("push")
    public <T extends WebhookContentBase<T>> void push(@RequestBody T content) {
    }
}
