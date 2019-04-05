package no.difi.meldingsutveksling.nextmove;

import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.receipt.ConversationService;
import no.difi.meldingsutveksling.receipt.GenericReceiptStatus;
import no.difi.meldingsutveksling.receipt.MessageStatus;

import java.time.LocalDateTime;

@Slf4j
public class TimeToLiveHelper {

    public static void registerErrorStatusAndMessage(StandardBusinessDocument sbd, ConversationService conversationService) {
        String errorMessage = String.format("ExpectedResponseDateTime (%s) is after current time. Message will not be handled further. Please resend...", sbd.getStandardBusinessDocumentHeader().getExpectedResponseDateTime());
        MessageStatus ms = MessageStatus.of(GenericReceiptStatus.FEIL, LocalDateTime.now(), errorMessage);
        conversationService.registerStatus(sbd.getConversationId(), ms);
        log.error(errorMessage);
    }
}
