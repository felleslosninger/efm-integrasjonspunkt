package no.difi.meldingsutveksling.dpi.json;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import no.difi.meldingsutveksling.status.MessageStatus;
import no.difi.meldingsutveksling.status.MessageStatusFactory;
import no.difi.meldingsutveksling.dpi.client.domain.messagetypes.DpiMessageType;

@RequiredArgsConstructor
public class MessageStatusMapper {

    private final MessageStatusFactory messageStatusFactory;

    public MessageStatus getMessageStatus(DpiMessageType dpiMessageType) {
        switch (dpiMessageType) {
            case LEVERINGSKVITTERING:
                return messageStatusFactory.getMessageStatus(ReceiptStatus.LEVERT, "Kvittering på at digital post er tilgjengeliggjort eller at en fysisk post er postlagt");
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

    public MessageStatus getMessageStatus(no.difi.meldingsutveksling.dpi.client.domain.MessageStatus in) {
        switch (in.getStatus()) {
            case OPPRETTET:
                return messageStatusFactory.getMessageStatus(ReceiptStatus.SENDT, "Hjørne 2 ha mottatt meldingen");
            case SENDT:
                return messageStatusFactory.getMessageStatus(ReceiptStatus.MOTTATT, "Hjørne 3 ha mottatt meldingen");
            case FEILET:
                return messageStatusFactory.getMessageStatus(ReceiptStatus.FEIL, "Generell melding om at det har skjedd en feil");
            default:
                return getDefaultMessageStatus();
        }
    }
}
