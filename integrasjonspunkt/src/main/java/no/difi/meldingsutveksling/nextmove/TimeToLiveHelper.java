package no.difi.meldingsutveksling.nextmove;

import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.receipt.ConversationService;
import no.difi.meldingsutveksling.receipt.MessageStatus;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;

import java.time.LocalDateTime;

@Slf4j
public class TimeToLiveHelper {

    public static void timeToLiveErrorMessage(StandardBusinessDocument sbd) {
        log.error("ExpectedResponseDateTime (%s) is after current time. Message will not be handled further. Please resend...", sbd.getExpectedResponseDateTime());
    }

    public static String expectedResponseDateTimeExpiredErrorMessage(StandardBusinessDocument sbd) {
        return String.format("Levetid for melding: %s er utgått. Må sendes på nytt", sbd.getExpectedResponseDateTime());
    }

    public static void registerErrorStatusAndMessage(StandardBusinessDocument sbd, ConversationService conversationService) {
         conversationService.registerStatus(sbd.getConversationId(), MessageStatus.of(ReceiptStatus.FEIL, LocalDateTime.now(), expectedResponseDateTimeExpiredErrorMessage(sbd)));
    }
}