package no.difi.meldingsutveksling.nextmove;

import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.receipt.ConversationService;
import no.difi.meldingsutveksling.receipt.MessageStatus;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Slf4j
public class TimeToLiveHelper {

    private final ConversationService conversationService;

    @Autowired
    public TimeToLiveHelper(ConversationService conversationService) {
        this.conversationService = conversationService;

    }

    public void registerErrorStatusAndMessage(StandardBusinessDocument sbd) {
        String status = String.format("Levetid for melding: %s er utgått. Må sendes på nytt", sbd.getExpectedResponseDateTime());
        conversationService.registerStatus(sbd.getConversationId(), MessageStatus.of(ReceiptStatus.FEIL, LocalDateTime.now(), status));
    }
}