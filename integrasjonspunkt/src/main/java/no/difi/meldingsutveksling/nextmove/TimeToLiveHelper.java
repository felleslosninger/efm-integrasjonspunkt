package no.difi.meldingsutveksling.nextmove;

import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocumentHeader;
import no.difi.meldingsutveksling.receipt.ConversationService;
import no.difi.meldingsutveksling.receipt.GenericReceiptStatus;
import no.difi.meldingsutveksling.receipt.MessageStatus;

import java.time.LocalDateTime;

@Slf4j
public class TimeToLiveHelper {

    public static void timeToLiveErrorMessage(StandardBusinessDocumentHeader header) {
        log.error("ExpectedResponseDateTime (%s) is after current time. Message will not be handled further. Please resend...", header.getExpectedResponseDateTime());
    }

    public static String expectedResponseDateTimeExpiredErrorMessage(StandardBusinessDocumentHeader header) {
        return String.format("Levetid for meldingen er utgått: %s . Må sendes på nytt", header.getExpectedResponseDateTime());
    }
    public static void registerErrorStatusAndMessage(StandardBusinessDocument sbd, ConversationService conversationService) {
        MessageStatus ms = MessageStatus.of(GenericReceiptStatus.FEIL, LocalDateTime.now(), expectedResponseDateTimeExpiredErrorMessage(sbd.getStandardBusinessDocumentHeader()));
        conversationService.registerStatus(sbd.getConversationId(), ms);
    }
}
