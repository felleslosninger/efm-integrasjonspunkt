package no.difi.meldingsutveksling.dpi.json;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.dpi.client.domain.messagetypes.DpiMessageType;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import no.difi.meldingsutveksling.status.MessageStatus;
import no.difi.meldingsutveksling.status.MessageStatusFactory;

@Slf4j
@RequiredArgsConstructor
public class MessageStatusMapper {

    private final MessageStatusFactory messageStatusFactory;

    public MessageStatus getMessageStatus(DpiMessageType dpiMessageType) {
        switch (dpiMessageType) {
            case LEVERINGSKVITTERING:
                return messageStatusFactory.getMessageStatus(ReceiptStatus.LEVERT, "Kvittering på at digital post er tilgjengeliggjort eller at fysisk post er klargjort for utskrift");
            case AAPNINGSKVITTERING:
                return messageStatusFactory.getMessageStatus(ReceiptStatus.LEST, "Kvittering fra Innbygger for at digital post er åpnet");
            case VARSLINGFEILETKVITTERING:
                return messageStatusFactory.getMessageStatus(ReceiptStatus.FEIL, "Kvittering for at en spesifisert varsling ikke har blitt sendt");
            case MOTTAKSKVITTERING:
                return messageStatusFactory.getMessageStatus(ReceiptStatus.LEVERT, "Kvittering fra utskrift og forsendelsestjenesten om at melding er mottatt og lagt til print");
            case RETURPOSTKVITTERING:
                return messageStatusFactory.getMessageStatus(ReceiptStatus.FEIL, "Kvittering fra utskrift og forsendelsestjenesten om at posten ikke har blitt levert til Mottaker.");
            case FEIL:
                return messageStatusFactory.getMessageStatus(ReceiptStatus.FEIL, "Generell melding om at det har skjedd en feil");
            default:
                return getDefaultMessageStatus();
        }
    }

    public MessageStatus getDefaultMessageStatus() {
        return messageStatusFactory.getMessageStatus(ReceiptStatus.ANNET, "Ukjent kvittering");
    }

    // status changes from corner 3 can be based on status or receipt, not all have a timestamp
    // if we have a timestamp from c3 we use it, if not we use the current time locally (we are c2)
    public MessageStatus getMessageStatus(no.difi.meldingsutveksling.dpi.client.domain.MessageStatus in) {
        log.debug("Received MessageStatus from corner 2 : {}", in);
        switch (in.getStatus()) {
            case OPPRETTET:
                return messageStatusFactory.getMessageStatus(ReceiptStatus.SENDT, "Hjørne 2 har mottatt meldingen");
            case SENDT:
                return (in.getTimestamp() == null) ? messageStatusFactory.getMessageStatus(ReceiptStatus.MOTTATT, "Hjørne 3 har mottatt meldingen") :
                    messageStatusFactory.getMessageStatus(ReceiptStatus.MOTTATT, "Hjørne 3 har mottatt meldingen", in.getTimestamp());
            case FEILET:
                return messageStatusFactory.getMessageStatus(ReceiptStatus.FEIL, "Generell melding om at det har skjedd en feil");
            default:
                return getDefaultMessageStatus();
        }
    }

}
