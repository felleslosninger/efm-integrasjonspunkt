package no.difi.meldingsutveksling.nextmove;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.receipt.ConversationService;
import no.difi.meldingsutveksling.receipt.MessageStatusFactory;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class TimeToLiveHelper {

    private final ConversationService conversationService;
    private final MessageStatusFactory messageStatusFactory;

    public void registerErrorStatusAndMessage(StandardBusinessDocument sbd) {
        String status = String.format("Levetid for melding: %s er utgått. Må sendes på nytt", sbd.getExpectedResponseDateTime());
        conversationService.registerStatus(sbd.getConversationId(), messageStatusFactory.getMessageStatus(ReceiptStatus.LEVETID_UTLOPT, status));
    }
}